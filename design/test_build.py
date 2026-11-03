"""Tests for design/build.py: the three layers build, and the core's checks still bite.

    python3 -m unittest discover -s design -p "test_*.py"
"""
import copy
import importlib
import sys
import unittest
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import build  # noqa: E402


def fresh_checks(t):
    importlib.reload(build.check_tokens)
    return build.checks(t)


class BuildTest(unittest.TestCase):
    def test_the_layers_build_and_pass(self):
        errors, _ = fresh_checks(build.assemble())
        self.assertEqual([], errors)
        text = build.kotlin(build.assemble())
        self.assertIn("package br.gabriel.cochilo.ui.theme", text)
        self.assertIn("Tatá Slings", text)

    def test_the_app_matches_the_layers(self):
        self.assertEqual(build.APP_TOKENS.read_text(), build.kotlin(build.assemble()))

    def test_a_brand_may_not_touch_a_data_colour(self):
        t = build.assemble()
        data = next(tok["name"] for tok in t["layers"]["cochilo"]["color"] if isinstance(tok, dict) and tok["name"].startswith("event"))
        t["brands"]["tata-slings"]["color"].append({"replaces": data, "value": {"light": "#000000", "dark": "#000000", "night": "#000000"}})
        errors, _ = fresh_checks(t)
        self.assertTrue(any("data colour" in e for e in errors), errors)

    def test_a_contrast_pair_under_its_minimum_fails(self):
        t = copy.deepcopy(build.assemble())
        t["contrast"].append({"fg": "page", "bg": "page", "min": 4.5, "layers": ["cochilo"]})
        errors, _ = fresh_checks(t)
        self.assertTrue(any("page on page" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
