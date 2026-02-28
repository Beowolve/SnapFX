// @ts-check

const config = {
  title: "SnapFX",
  tagline: "Lightweight JavaFX Docking Framework",
  url: "https://snapfx.org",
  baseUrl: "/",
  onBrokenLinks: "throw",
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: "warn"
    }
  },
  favicon: "img/snapfx.svg",
  organizationName: "Beowolve",
  projectName: "SnapFX",
  trailingSlash: false,
  presets: [
    [
      "classic",
      {
        docs: {
          routeBasePath: "/",
          sidebarPath: "./sidebars.js",
          editUrl: "https://github.com/Beowolve/SnapFX/tree/main/website/"
        },
        blog: false,
        pages: false,
        theme: {
          customCss: "./src/css/custom.css"
        }
      }
    ]
  ],
  themeConfig: {
    image: "img/snapfx.svg",
    navbar: {
      title: "SnapFX",
      logo: {
        alt: "SnapFX Logo",
        src: "img/snapfx.svg"
      },
      items: [
        {
          type: "docSidebar",
          sidebarId: "docsSidebar",
          position: "left",
          label: "Documentation"
        },
        {
          href: "https://snapfx.org/api/",
          label: "API",
          position: "left"
        },
        {
          href: "https://github.com/Beowolve/SnapFX",
          label: "GitHub",
          position: "right"
        }
      ]
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Docs",
          items: [
            {
              label: "Get Started",
              to: "/getting-started"
            },
            {
              label: "Architecture",
              to: "/architecture"
            }
          ]
        },
        {
          title: "Reference",
          items: [
            {
              label: "API JavaDoc",
              href: "https://snapfx.org/api/"
            },
            {
              label: "Architecture Decisions (ADR)",
              href: "https://github.com/Beowolve/SnapFX/tree/main/docs/adr"
            }
          ]
        },
        {
          title: "Community",
          items: [
            {
              label: "GitHub Issues",
              href: "https://github.com/Beowolve/SnapFX/issues"
            }
          ]
        }
      ],
      copyright: `Copyright © ${new Date().getFullYear()} SnapFX contributors`
    }
  }
};

module.exports = config;
