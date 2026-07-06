# ACE FlowSmith AI → IBM Bob: Capability Map

This maps the original FlowSmith idea (`initial.txt`) to what **IBM Bob** does natively in
the ACE Toolkit, and shows which part of this repo implements each piece.

## The idea, feature by feature

| # | Idea (from `initial.txt`) | Possible with Bob? | How / where |
|---|---|---|---|
| 1 | Learn org standards (logging, error handling, patterns, policies) | ✅ Native | Bob **custom mode** + `rules-flowsmith/*.xml` governance layer |
| 2 | Trained on reusable subflows, naming conventions, best practices | ✅ | Bob **skill** `flowsmith-patterns` (`SKILL.md` + per-pattern docs) over `Existing_Templates/` |
| 3 | Plain-text requirement → recommend the right pattern | ✅ Semantic (beats keyword matcher) | `SKILL.md` "how to select a pattern" + Bob's analyze step |
| 4 | Auto-generate message flows + ESQL in the Toolkit | ✅ Core `ace-bob` capability | Bob copies & tokenises the reference project |
| 5 | Enforce enterprise consistency | ✅ | `rules-flowsmith/*.xml` + Bob's validate step |
| 6 | Generate BAR files, server configs, deployment policies | ✅ | Bob Shell runs `ibmint`/`mqsi` (see `setup.md`) |
| 7 | Env-specific params auto-configured (DEV/ACC/PRO) | ✅ | `rules-flowsmith/4_env_config_dev_acc_pro.xml` |
| 8 | Developer acts as reviewer before deploy | ✅ Native | `custom_modes.yaml` approval flags |
| 9 | Reduce onboarding + delivery time | ✅ Outcome | end-to-end effect |

**Verdict:** the entire idea is expressible as **one Bob mode + one Bob skill + four rule
files**, backed by the existing template projects — no bespoke engine required.

## The "search engine" question, specifically

> Give a requirement in plain text; Bob checks which template is suitable — possible?

**Yes.** The legacy MVP scores keywords (`flowsmith/catalog.json` `whenToUse` arrays,
`flowsmith.py` scorer, `index.html` `scorePattern()`). Bob replaces that with **semantic
intent matching** defined in `SKILL.md`:

1. Bob reads the requirement.
2. It reasons over the four pattern docs (`ptp_file`, `pub_file`, `sub_file_online`,
   `sub_file_batch`) — matching *intent* (direction + cadence), not literal words.
3. It returns the best pattern, the runner-up, and a one-line reason.

This is strictly more capable than keyword scoring: "pick up files and move them to
another folder" correctly resolves to PTP even though it never says "point-to-point".

**Scaling note:** for ~4–40 patterns, describing them in the skill is enough. For hundreds
of templates, add a retrieval step (an MCP tool / vector index) that Bob queries first —
Bob supports MCP servers, so this is an add-on, not a redesign.

## What Bob still needs underneath

Bob is the **agent + IDE layer**; it consumes a **foundation model** (IBM Granite /
watsonx-served). So the runtime stack is:

```
plain-text requirement
        ↓
IBM Bob (agent, in ACE Toolkit)  ──uses──▶  Granite / watsonx.ai model
        ↓
flowsmith-ace-developer mode  +  flowsmith-patterns skill  +  rules-flowsmith
        ↓
tokenised ACE application (flows, ESQL, DEV/ACC/PRO configs)
        ↓
Bob Shell:  ibmint (build BAR)  →  mqsi (deploy to local Integration Server)
```
