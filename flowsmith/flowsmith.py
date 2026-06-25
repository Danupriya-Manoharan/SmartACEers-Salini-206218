#!/usr/bin/env python3
"""
ACE FlowSmith - MVP
===================
An intelligent-agent MVP for accelerating IBM App Connect Enterprise (ACE)
development on top of an organisation's existing reusable pattern templates.

It does three things:

  1. list        - print the pattern "map" (catalog of reusable templates)
  2. recommend   - match a plain-English functional requirement to a pattern
  3. generate    - instantiate a chosen pattern into a ready-to-import ACE
                   application + environment configs, with all organisation
                   tokens (SUBSYS / APPNM / FUNCNM / NDMNM) substituted in
                   folder names, file names AND file contents.

This is the cross-platform, offline successor to CreatePatternAppl.pl:
no Perl, no Windows, no git clone from an internal server required.

The developer remains the reviewer: FlowSmith generates, the human validates
and fine-tunes before deployment.

Usage
-----
  python3 flowsmith.py list
  python3 flowsmith.py recommend "consume messages from a queue and write files"
  python3 flowsmith.py generate --pattern ptp_file \
        --subsys XAJ --app TLMTF --func FINANCING [--ndm REFPGN] [--out ../Generated]
  python3 flowsmith.py generate --requirements requirements.sample.json
"""

import argparse
import json
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(HERE)                       # SmartACEers-Salini-206218
CATALOG_PATH = os.path.join(HERE, "catalog.json")
DEFAULT_OUT = os.path.join(REPO_ROOT, "Generated")

# Tokens that are replaced everywhere (dir names, file names, file contents).
# Order matters only for readability; none is a substring of another.
TOKEN_KEYS = ["SUBSYS", "APPNM", "FUNCNM", "NDMNM"]

# File extensions we treat as text and rewrite the contents of.
TEXT_EXT = {
    ".msgflow", ".esql", ".subflow", ".prop", ".properties", ".project",
    ".descriptor", ".xml", ".map", ".mset", ".mxsd", ".xsd", ".json", ".txt",
}
# Dotfiles / extensionless config files that splitext() can't classify.
TEXT_NAMES = {".project", ".classpath"}


# --------------------------------------------------------------------------- #
# Catalog helpers
# --------------------------------------------------------------------------- #
def load_catalog():
    with open(CATALOG_PATH, encoding="utf-8") as fh:
        return json.load(fh)


def find_pattern(catalog, pattern_id):
    for p in catalog["patterns"]:
        if p["id"] == pattern_id:
            return p
    return None


# --------------------------------------------------------------------------- #
# Command: list
# --------------------------------------------------------------------------- #
def cmd_list(args):
    catalog = load_catalog()
    print("\nACE FlowSmith - Pattern Map (%d reusable templates)\n" % len(catalog["patterns"]))
    print("  %-20s %-6s %-12s %s" % ("ID", "TYPE", "CONNECT", "TITLE"))
    print("  " + "-" * 76)
    for p in catalog["patterns"]:
        print("  %-20s %-6s %-12s %s" % (
            p["id"], p["integrationType"], p["connectivity"], p["title"]))
    print("\nTokens substituted on generation:")
    for k, v in catalog["tokens"].items():
        print("  %-8s %s" % (k, v))
    print("\nTarget environments: %s\n" % ", ".join(catalog["environments"]))
    return 0


# --------------------------------------------------------------------------- #
# Command: recommend  (the seam where a real LLM plugs in)
# --------------------------------------------------------------------------- #
def score_pattern(pattern, requirement):
    """Lightweight keyword-overlap scorer. Deterministic and offline.
    In the full product this is replaced by an LLM classification call, but the
    contract (requirement text -> ranked pattern ids) stays identical."""
    text = requirement.lower()
    score = 0
    hits = []
    for kw in pattern.get("whenToUse", []):
        if kw in text:
            score += 2 + kw.count(" ")     # multi-word phrase matches weigh more
            hits.append(kw)
    # Light boost for naming the integration type explicitly.
    itype = pattern["integrationType"].lower()
    if re.search(r"\b%s\b" % re.escape(itype), text):
        score += 1
        hits.append(itype)
    return score, hits


def cmd_recommend(args):
    catalog = load_catalog()
    requirement = args.requirement.strip()
    if not requirement:
        print("Provide a requirement, e.g. recommend \"file to file transfer\"")
        return 2

    ranked = []
    for p in catalog["patterns"]:
        score, hits = score_pattern(p, requirement)
        ranked.append((score, p, hits))
    ranked.sort(key=lambda t: t[0], reverse=True)

    print("\nRequirement: %s\n" % requirement)
    if ranked[0][0] == 0:
        print("No confident match. Showing the full map - pick a pattern id with --pattern.\n")
        return cmd_list(args)

    print("Recommended patterns (best first):\n")
    for score, p, hits in ranked:
        if score == 0:
            continue
        bar = "*" * min(score, 10)
        print("  [%2d] %-10s %-22s %s" % (score, bar, p["id"], p["title"]))
        print("       matched: %s" % (", ".join(hits) if hits else "-"))
    best = ranked[0][1]
    print("\nTop pick: %s  (%s)" % (best["id"], best["title"]))
    print("Generate it with:")
    print("  python3 flowsmith.py generate --pattern %s --subsys XAJ --app TLMTF --func FINANCING\n"
          % best["id"])
    return 0


# --------------------------------------------------------------------------- #
# Command: generate
# --------------------------------------------------------------------------- #
def build_replacements(values):
    """values: dict with keys subsys/app/func/ndm -> token replacement map."""
    repl = {}
    if values.get("subsys"):
        repl["SUBSYS"] = values["subsys"].upper()
    if values.get("app"):
        repl["APPNM"] = values["app"]
    if values.get("func"):
        repl["FUNCNM"] = values["func"]
    # "NONE" (any case) is a sentinel meaning "no NDM" — lets callers always
    # pass --ndm (e.g. from a Toolkit prompt) without supplying a real value.
    ndm = values.get("ndm")
    if ndm and ndm.strip().upper() != "NONE":
        repl["NDMNM"] = ndm.upper()
    return repl


def apply_tokens(name, repl):
    for token, value in repl.items():
        name = name.replace(token, value)
    return name


def is_text_file(path):
    base = os.path.basename(path)
    return base in TEXT_NAMES or os.path.splitext(path)[1].lower() in TEXT_EXT


def rewrite_contents(path, repl):
    try:
        with open(path, encoding="utf-8") as fh:
            content = fh.read()
    except (UnicodeDecodeError, IsADirectoryError):
        return 0
    new = content
    for token, value in repl.items():
        new = new.replace(token, value)
    if new != content:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(new)
        return sum(content.count(t) for t in repl)
    return 0


def rename_tree(root, repl):
    """Bottom-up rename of directories and files that contain tokens."""
    renamed = 0
    for dirpath, dirnames, filenames in os.walk(root, topdown=False):
        for fn in filenames:
            new = apply_tokens(fn, repl)
            if new != fn:
                os.rename(os.path.join(dirpath, fn), os.path.join(dirpath, new))
                renamed += 1
        # directory itself
        base = os.path.basename(dirpath)
        new_base = apply_tokens(base, repl)
        if new_base != base:
            os.rename(dirpath, os.path.join(os.path.dirname(dirpath), new_base))
            renamed += 1
    return renamed


def cmd_generate(args):
    catalog = load_catalog()

    # Inputs can come from a JSON requirements file or from CLI flags.
    values = {}
    pattern_id = args.pattern
    if args.requirements:
        with open(args.requirements, encoding="utf-8") as fh:
            req = json.load(fh)
        pattern_id = pattern_id or req.get("pattern")
        values = {k: req.get(k) for k in ("subsys", "app", "func", "ndm")}
        # Allow a free-text requirement to auto-select the pattern.
        if not pattern_id and req.get("requirement"):
            ranked = sorted(
                ((score_pattern(p, req["requirement"])[0], p) for p in catalog["patterns"]),
                key=lambda t: t[0], reverse=True)
            if ranked and ranked[0][0] > 0:
                pattern_id = ranked[0][1]["id"]
                print("Auto-selected pattern '%s' from requirement text." % pattern_id)
    # CLI flags override / fill in.
    for k in ("subsys", "app", "func", "ndm"):
        if getattr(args, k):
            values[k] = getattr(args, k)

    if not pattern_id:
        print("ERROR: no pattern chosen. Use --pattern <id> or 'recommend' first.")
        return 2
    pattern = find_pattern(catalog, pattern_id)
    if not pattern:
        print("ERROR: unknown pattern '%s'. Run 'list' to see options." % pattern_id)
        return 2

    # Validate required tokens.
    missing = [t for t in pattern["requiredTokens"]
               if not values.get({"SUBSYS": "subsys", "APPNM": "app", "FUNCNM": "func"}[t])]
    if missing:
        print("ERROR: pattern '%s' requires: %s" % (pattern_id, ", ".join(missing)))
        print("Provide --subsys / --app / --func accordingly.")
        return 2

    repl = build_replacements(values)
    src = os.path.join(REPO_ROOT, pattern["templateDir"])
    if not os.path.isdir(src):
        print("ERROR: template not found: %s" % src)
        return 2

    # Output application name, e.g. XAJ_PTP_TLMTF_FINANCING_FIL
    app_project = apply_tokens(pattern["appProject"], repl)
    out_root = args.out or DEFAULT_OUT
    dest = os.path.join(out_root, app_project)
    if os.path.exists(dest):
        if not args.force:
            print("ERROR: %s already exists. Use --force to overwrite." % dest)
            return 2
        shutil.rmtree(dest)

    # 1. Copy the template skeleton.
    shutil.copytree(src, dest, ignore=shutil.ignore_patterns(".DS_Store", ".git"))

    # 2. Rewrite file contents.
    content_hits = 0
    files_changed = 0
    for dirpath, _, filenames in os.walk(dest):
        for fn in filenames:
            path = os.path.join(dirpath, fn)
            if is_text_file(path):
                n = rewrite_contents(path, repl)
                if n:
                    content_hits += n
                    files_changed += 1

    # 3. Rename folders and files.
    renamed = rename_tree(dest, repl)

    # Report - the developer reviews this before importing into ACE Toolkit.
    print("\n=== ACE FlowSmith :: generation complete ===")
    print("Pattern        : %s  (%s)" % (pattern["id"], pattern["title"]))
    print("Integration    : %s / %s" % (pattern["integrationType"], pattern["connectivity"]))
    print("Tokens applied : %s" % ", ".join("%s=%s" % (k, v) for k, v in repl.items()))
    print("Output project : %s" % dest)
    print("Substitutions  : %d token hits across %d files; %d paths renamed"
          % (content_hits, files_changed, renamed))
    print("\nGenerated tree:")
    _print_tree(dest, out_root)
    print("\nNext steps (developer as reviewer):")
    print("  1. Import '%s' into ACE Toolkit (File > Import > Existing project)." % app_project)
    print("  2. Review the .msgflow / .esql and fine-tune business logic.")
    print("  3. Validate env configs under *_Configs/batchconfig/{DEV,ACC,PRO}.")
    print("  4. Build the BAR and deploy.\n")
    return 0


def _print_tree(root, base, prefix="  "):
    print(prefix + os.path.basename(root) + "/")
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames.sort()
        for fn in sorted(filenames):
            if fn == ".DS_Store":
                continue
            r = os.path.relpath(os.path.join(dirpath, fn), root)
            print(prefix + "    " + r)


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #
def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="flowsmith",
        description="ACE FlowSmith MVP - discover and instantiate reusable ACE patterns.")
    sub = parser.add_subparsers(dest="command")

    sub.add_parser("list", help="Show the pattern map / catalog.")

    pr = sub.add_parser("recommend", help="Recommend a pattern from a plain-English requirement.")
    pr.add_argument("requirement", help="Free-text functional requirement.")

    pg = sub.add_parser("generate", help="Instantiate a pattern into a ready-to-import ACE app.")
    pg.add_argument("--pattern", help="Pattern id (see 'list').")
    pg.add_argument("--subsys", help="Subsystem code, e.g. XAJ")
    pg.add_argument("--app", help="Application code, e.g. TLMTF")
    pg.add_argument("--func", help="Functionality, e.g. FINANCING")
    pg.add_argument("--ndm", help="NDM name (optional), e.g. REFPGN")
    pg.add_argument("--requirements", help="JSON file with pattern + token values.")
    pg.add_argument("--out", help="Output directory (default: ../Generated).")
    pg.add_argument("--force", action="store_true", help="Overwrite existing output.")

    args = parser.parse_args(argv)
    if not args.command:
        parser.print_help()
        return 0
    return {"list": cmd_list, "recommend": cmd_recommend, "generate": cmd_generate}[args.command](args)


if __name__ == "__main__":
    sys.exit(main())
