"""Generate a Markdown coverage summary from a JaCoCo report XML.

Standard library only. Run: python3 scripts/coverage_summary.py <jacoco.xml> <output.md>
"""
import sys
import xml.etree.ElementTree as ElementTree

COUNTER_TYPES = ("LINE", "BRANCH", "INSTRUCTION", "METHOD", "CLASS")


def _counters(element):
    """Return {counter type: (covered, total)} for the <counter> children of *element*."""
    result = {}
    for counter in element.findall("counter"):
        covered = int(counter.get("covered", 0))
        missed = int(counter.get("missed", 0))
        result[counter.get("type")] = (covered, covered + missed)
    return result


def totals(path):
    """Return {counter type: (covered, total)} for root-level <counter> elements."""
    return _counters(ElementTree.parse(path).getroot())


def package_rows(path):
    """Return sorted [(name, LINE (covered, total), BRANCH (covered, total)), ...]."""
    root = ElementTree.parse(path).getroot()
    rows = []
    for package in root.findall("package"):
        counters = _counters(package)
        name = package.get("name", "").replace("/", ".")
        rows.append((name, counters.get("LINE", (0, 0)), counters.get("BRANCH", (0, 0))))
    rows.sort(key=lambda row: row[0])
    return rows


def percent(pair):
    """Return coverage percent with one decimal, or "n/a" when the total is 0."""
    covered, total = pair
    if total == 0:
        return "n/a"
    return f"{covered / total * 100:.1f}%"


def markdown(path, threshold=0.70):
    """Build the full Markdown summary for the JaCoCo report at *path*."""
    summary = totals(path)
    rows = package_rows(path)

    def counter_line(ctype):
        covered, total = summary.get(ctype, (0, 0))
        return f"| {ctype.capitalize()} | {covered} / {total} | {percent((covered, total))} |"

    line_covered, line_total = summary.get("LINE", (0, 0))
    line_ratio = line_covered / line_total if line_total else 0.0
    minimum = round(threshold * 100)
    line_pct = percent((line_covered, line_total))
    if line_ratio >= threshold:
        result = f"**PASS**: line coverage {line_pct} meets the {minimum}% rubric minimum."
    else:
        result = f"**FAIL**: line coverage {line_pct} is below the {minimum}% rubric minimum."

    lines = [
        "# Backend test coverage",
        "",
        "Generated from `docs/coverage/jacoco.xml` by `scripts/coverage-report.sh` (JaCoCo, `./mvnw verify`).",
        "",
        result,
        "",
        "| Counter | Covered / total | Coverage |",
        "|---|---|---|",
    ]
    lines.extend(counter_line(ctype) for ctype in COUNTER_TYPES)
    lines.extend(
        [
            "",
            "## By package",
            "",
            "| Package | Lines | Line % | Branches | Branch % |",
            "|---|---|---|---|---|",
        ]
    )
    for name, line_pair, branch_pair in rows:
        lines.append(
            f"| {name} | {line_pair[0]} / {line_pair[1]} | {percent(line_pair)} | "
            f"{branch_pair[0]} / {branch_pair[1]} | {percent(branch_pair)} |"
        )
    lines.append("")
    lines.append(
        "The full HTML report is uploaded by CI as the `jacoco-report` artifact, "
        "or run `scripts/coverage-report.sh` and open `backend/target/site/jacoco/index.html`."
    )
    return "\n".join(lines) + "\n"


def main(argv=None):
    """Write `markdown(xml)` to the output file; exit 1 on FAIL, else 0."""
    if argv is None:
        argv = sys.argv[1:]
    xml_path, output_path = argv[0], argv[1]
    text = markdown(xml_path)
    with open(output_path, "w", encoding="utf-8") as handle:
        handle.write(text)
    return 1 if "**FAIL**:" in text else 0


if __name__ == "__main__":
    sys.exit(main())
