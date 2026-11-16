#!/usr/bin/env python3
"""Build a review submission ZIP. Never includes signing keys or Gradle caches."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import subprocess
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--libraries', required=True, type=Path, help='Exported releaseRuntimeClasspath binaries')
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
source_paths = ['app', 'gradle', 'scripts', '.github', '.gitignore', 'README.md', 'build.gradle.kts',
                'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat']
source = subprocess.check_output(['git', 'archive', '--format=zip', '--prefix=HealthMate_Source/',
                                  revision, '--', *source_paths], cwd=root)
files = {'HealthMate_Source.zip': source}
for local, target in [
    ('docs/HealthMate_Technical_Report_Draft.pdf', 'HealthMate_Technical_Report_REVIEW.pdf'),
    ('docs/HealthMate_Technical_Report_Draft.docx', 'HealthMate_Technical_Report_EDITABLE.docx'),
    ('releases/HealthMate-0.1.1-release.apk', 'HealthMate-0.1.1-release.apk'),
    ('releases/signature-verification.txt', 'Evidence/signature-verification.txt'),
    ('docs/report-evidence/test-summary.json', 'Evidence/test-summary.json'),
    ('docs/submission/HealthMate_Recording_and_Submission_Guide.pdf', 'Recording_and_Submission_Guide.pdf'),
    ('docs/submission/Submission_Checklist.md', 'Submission_Checklist.md'),
]:
    files[target] = (root / local).read_bytes()
for evidence in (root / 'docs/submission').glob('*verification*.md'):
    files['Evidence/' + evidence.name] = evidence.read_bytes()
for library in sorted(args.libraries.iterdir()):
    if library.is_file() and library.suffix.lower() in {'.jar', '.aar'}:
        files['Runtime_Libraries/' + library.name] = library.read_bytes()
if not any(p.startswith('Runtime_Libraries/') for p in files):
    raise SystemExit('No runtime libraries found; refusing incomplete library package.')
files['Runtime_Libraries/README.txt'] = (
    'Original releaseRuntimeClasspath JAR/AAR dependencies, including their embedded notices.\n'
    'Third-party licences remain applicable. Refer to Gradle manifests for dependency declarations.\n'
    'This directory does not include build-time tooling or provide a complete offline Gradle repository.\n'
).encode()
files['READ_ME_FIRST.txt'] = (
    'HEALTHMATE - Navod Daham - K2635839 - REVIEW PACKAGE\n'
    f'Source revision: {revision}\n'
    'NOT READY FOR FINAL SUBMISSION: supply video and APK Google Drive Viewer links,\n'
    'complete personal reflection/contributions and confirm declarations after review.\n'
    'Replace the report draft with your reviewed final PDF before submitting via ELMS.\n'
    'The narrated video is not included. Keep it <=25 MB and use your own voice.\n'
    'Private signing keys/passwords are deliberately excluded.\n'
    'Open the extracted source project in Android Studio. Requires Android SDK 35 and JDK 17.\n'
).encode()
files['MANIFEST.json'] = json.dumps({'source_revision': revision, 'review_only': True,
    'files': [{'path': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
              for name, data in sorted(files.items())]}, indent=2).encode()
args.output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(args.output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for name, data in sorted(files.items()):
        archive.writestr('HealthMate_K2635839_REVIEW/' + name, data)
with zipfile.ZipFile(args.output) as archive:
    assert archive.testzip() is None
print(f'Created and integrity-checked {args.output}: {args.output.stat().st_size:,} bytes')
