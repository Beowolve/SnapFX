package org.snapfx.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockLayoutSnapshotServiceTest {

    @Test
    void createSnapshotJsonWrapsMainLayoutAndFloatingWindows() {
        DockLayoutSnapshotService service = new DockLayoutSnapshotService();
        JsonArray floatingWindows = new JsonArray();
        floatingWindows.add(service.createFloatingWindowEntry(
            validLayoutJson(),
            100.0,
            200.0,
            640.0,
            420.0,
            true
        ));

        String snapshotJson = service.createSnapshotJson(validLayoutJson(), floatingWindows);
        JsonObject parsed = JsonParser.parseString(snapshotJson).getAsJsonObject();

        assertTrue(parsed.has("mainLayout"));
        assertTrue(parsed.has("floatingWindows"));
        assertEquals(1, parsed.getAsJsonArray("floatingWindows").size());
    }

    @Test
    void tryParseSnapshotReturnsNullForLegacyLayoutJson() {
        DockLayoutSnapshotService service = new DockLayoutSnapshotService();

        assertNull(service.tryParseSnapshot(validLayoutJson()));
        assertNull(service.tryParseSnapshot("{ invalid"));
    }

    @Test
    void tryParseSnapshotParsesFloatingWindowEntries() {
        DockLayoutSnapshotService service = new DockLayoutSnapshotService();
        JsonArray floatingWindows = new JsonArray();
        floatingWindows.add(service.createFloatingWindowEntry(
            validLayoutJson(),
            120.0,
            300.0,
            500.0,
            350.0,
            false
        ));

        String snapshotJson = service.createSnapshotJson(validLayoutJson(), floatingWindows);
        DockLayoutSnapshotService.DockLayoutSnapshot snapshot = service.tryParseSnapshot(snapshotJson);

        assertNotNull(snapshot);
        assertEquals(1, snapshot.floatingWindows().size());
        DockLayoutSnapshotService.DockFloatingWindowSnapshot floatingSnapshot = snapshot.floatingWindows().getFirst();
        assertEquals(120.0, floatingSnapshot.x(), 0.0001);
        assertEquals(300.0, floatingSnapshot.y(), 0.0001);
        assertEquals(500.0, floatingSnapshot.width(), 0.0001);
        assertEquals(350.0, floatingSnapshot.height(), 0.0001);
        assertEquals(false, floatingSnapshot.alwaysOnTop());
    }

    @Test
    void validateLayoutJsonRebasesErrorPath() {
        DockLayoutSnapshotService service = new DockLayoutSnapshotService();
        String invalidLayout = """
            {
              "locked": false,
              "layoutIdCounter": 1,
              "root": {
                "id": "dock-1",
                "dockNodeId": "main",
                "type": "DockNode",
                "closeable": true
              }
            }
            """;

        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> service.validateLayoutJson(invalidLayout, "$.mainLayout", null)
        );

        assertEquals("$.mainLayout.root.title", exception.getLocation());
        assertTrue(exception.getMessage().contains("Missing required field"));
    }

    @Test
    void validateSnapshotRejectsMissingFloatingLayout() {
        DockLayoutSnapshotService service = new DockLayoutSnapshotService();
        DockLayoutSnapshotService.DockLayoutSnapshot snapshot = new DockLayoutSnapshotService.DockLayoutSnapshot(
            JsonParser.parseString(validLayoutJson()).getAsJsonObject(),
            java.util.List.of(new DockLayoutSnapshotService.DockFloatingWindowSnapshot(null, null, null, null, null, null))
        );

        DockLayoutLoadException exception = assertThrows(
            DockLayoutLoadException.class,
            () -> service.validateSnapshot(snapshot, null)
        );

        assertEquals("$.floatingWindows[0].layout", exception.getLocation());
    }

    private String validLayoutJson() {
        return """
            {
              "locked": false,
              "layoutIdCounter": 1,
              "root": {
                "id": "dock-1",
                "dockNodeId": "main",
                "type": "DockNode",
                "title": "Main",
                "closeable": true
              }
            }
            """;
    }
}
