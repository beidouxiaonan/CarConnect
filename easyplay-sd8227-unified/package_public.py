"""Backward-compatible entry point for the current authored-source bundle."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('package_unified.py')),run_name='__main__')
