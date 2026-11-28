"""Tests for coverage_summary.py (SSP1-151). Run: python3 -m unittest scripts/test_coverage_summary.py"""
import os
import sys
import tempfile
import unittest
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import coverage_summary  # noqa: E402
SAMPLE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<report name="backend">
<package name="com/Handoff/backend/service">
<counter type="INSTRUCTION" missed="10" covered="90"/>
<counter type="BRANCH" missed="2" covered="8"/>
<counter type="LINE" missed="5" covered="45"/>
<counter type="METHOD" missed="1" covered="9"/>
<counter type="CLASS" missed="0" covered="3"/>
</package>
<package name="com/Handoff/backend/controller">
<counter type="INSTRUCTION" missed="40" covered="60"/>
<counter type="BRANCH" missed="5" covered="5"/>
<counter type="LINE" missed="20" covered="30"/>
<counter type="METHOD" missed="2" covered="8"/>
<counter type="CLASS" missed="0" covered="2"/>
</package>
<counter type="INSTRUCTION" missed="50" covered="150"/>
<counter type="BRANCH" missed="7" covered="13"/>
<counter type="LINE" missed="25" covered="75"/>
<counter type="METHOD" missed="3" covered="17"/>
<counter type="CLASS" missed="0" covered="5"/>
</report>
"""
class CoverageSummaryTest(unittest.TestCase):
    def setUp(self):
        handle = tempfile.NamedTemporaryFile("w", suffix=".xml", delete=False)
        handle.write(SAMPLE)
        handle.close()
        self.path = handle.name

    def tearDown(self):
        os.unlink(self.path)

    def test_reads_the_report_totals(self):
        totals = coverage_summary.totals(self.path)
        self.assertEqual(totals["LINE"], (75, 100))
        self.assertEqual(totals["BRANCH"], (13, 20))

    def test_reports_each_package_sorted_by_name(self):
        rows = coverage_summary.package_rows(self.path)
        self.assertEqual([row[0] for row in rows], ["com.Handoff.backend.controller", "com.Handoff.backend.service"])
        self.assertEqual(rows[0][1], (30, 50))

    def test_formats_a_percentage_with_one_decimal(self):
        self.assertEqual(coverage_summary.percent((75, 100)), "75.0%")
        self.assertEqual(coverage_summary.percent((1, 3)), "33.3%")
        self.assertEqual(coverage_summary.percent((0, 0)), "n/a")

    def test_markdown_states_the_result_against_the_threshold(self):
        text = coverage_summary.markdown(self.path, threshold=0.70)
        self.assertIn("# Backend test coverage", text)
        self.assertIn("| Line | 75 / 100 | 75.0% |", text)
        self.assertIn("**PASS**: line coverage 75.0% meets the 70% rubric minimum.", text)
        self.assertIn("| com.Handoff.backend.controller | 30 / 50 | 60.0% | 5 / 10 | 50.0% |", text)

    def test_markdown_reports_a_failure_below_the_threshold(self):
        text = coverage_summary.markdown(self.path, threshold=0.80)
        self.assertIn("**FAIL**: line coverage 75.0% is below the 80% rubric minimum.", text)

    def test_markdown_separates_blocks_with_blank_lines(self):
        # GitHub only renders a table that starts after a blank line.
        text = coverage_summary.markdown(self.path, threshold=0.70)
        self.assertTrue(text.startswith("# Backend test coverage\n\nGenerated from"))
        self.assertIn("rubric minimum.\n\n| Counter | Covered / total | Coverage |\n|---|---|---|\n| Line |", text)
        self.assertIn("|\n\n## By package\n\n| Package | Lines |", text)
        self.assertIn("|\n\nThe full HTML report", text)
        self.assertNotIn("\n\n\n", text)

    def test_main_writes_the_file_and_signals_fail_with_exit_code_1(self):
        out = tempfile.NamedTemporaryFile("w", suffix=".md", delete=False)
        out.close()
        try:
            self.assertEqual(coverage_summary.main([self.path, out.name]), 0)
            with open(out.name, encoding="utf-8") as handle:
                self.assertIn("**PASS**", handle.read())
            low = tempfile.NamedTemporaryFile("w", suffix=".xml", delete=False)
            low.write(SAMPLE.replace('type="LINE" missed="25" covered="75"', 'type="LINE" missed="50" covered="50"'))
            low.close()
            try:
                self.assertEqual(coverage_summary.main([low.name, out.name]), 1)
            finally:
                os.unlink(low.name)
        finally:
            os.unlink(out.name)

if __name__ == "__main__":
    unittest.main()
