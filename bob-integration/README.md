# FlowSmith → IBM Bob Integration

This folder makes the **ACE FlowSmith AI** idea real using **IBM Bob** — IBM's agentic
AI development partner that runs *inside the IBM App Connect Enterprise (ACE) Toolkit*.

Nothing in the rest of the repository is modified by this folder. It is a **self-contained
Bob configuration** (one custom *mode* + one *skill* + governance *rules*) that you drop
into an ACE Toolkit workspace so Bob can take a plain-text requirement and produce a
standards-compliant, deployable ACE application.

---

## Why Bob (instead of the hand-built engine)

The original MVP recommends a pattern with a **keyword matcher** (`flowsmith/flowsmith.py`,
`mvp-web/index.html`) and instantiates templates with a Perl script. IBM Bob replaces that
whole engine with a native, semantic one:

| FlowSmith building block | Bob equivalent (in this folder) |
|---|---|
| Keyword matcher over `catalog.json` | `skills/flowsmith-patterns/SKILL.md` — Bob matches requirements to patterns *semantically* |
| Perl template instantiation | Bob reads the reference templates and writes the new project |
| Org naming / logging conventions | `rules-flowsmith/*.xml` — enforced automatically |
| `compile-and-run.bat` build & deploy | Bob Shell runs `ibmint` / `mqsi` commands directly |
| Developer reviews before deploy | Bob's built-in approval mode |

Bob does **not** remove the need for a foundation model — it *uses* one underneath
(IBM Granite / watsonx-served models). So the stack is: **your requirement → Bob (agent) →
Granite model → this skill+mode → ACE artifacts → Bob Shell build & deploy.**

---

## What's in here

```
bob-integration/
├── README.md                          ← you are here
├── .bob/
│   ├── custom_modes.yaml              ← defines the "FlowSmith ACE Developer" mode
│   ├── rules-flowsmith/               ← your enterprise standards, enforced by Bob
│   │   ├── 1_naming_conventions.xml
│   │   ├── 2_logging_and_shared_libraries.xml
│   │   ├── 3_esql_and_coding_standards.xml
│   │   └── 4_env_config_dev_acc_pro.xml
│   └── skills/
│       └── flowsmith-patterns/        ← the pattern catalog as a Bob skill
│           ├── SKILL.md               ← how Bob picks a pattern from plain text
│           ├── ptp_file.md
│           ├── pub_file.md
│           ├── sub_file_online.md
│           └── sub_file_batch.md
└── docs/
    ├── idea-to-bob-map.md             ← full idea → Bob capability mapping
    └── setup.md                       ← install & run steps
```

The patterns and tokens are derived directly from `flowsmith/catalog.json` and the
reference projects under `Existing_Templates/`, which remain the source of truth Bob reads.

See [`docs/setup.md`](docs/setup.md) to install and [`docs/idea-to-bob-map.md`](docs/idea-to-bob-map.md)
for the complete capability breakdown.
