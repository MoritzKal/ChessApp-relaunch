# PR Summary

**Title:** docs: bootstrap ssot docs and ci

## Changes
- establish SSOT docs (overview, roadmap, state, endpoints, observability)
- add role descriptions and chat template
- provide docs tooling, CI workflow, and Makefile wrappers
- remove duplicated API/observability details and add report
- sync API and metrics docs with extracted code definitions
- fix relative doc links in README and duplicate report

## DoD Checklist
- [x] `make -f Makefile.docs fmt`
- [ ] `make -f Makefile.docs lint` *(fails: 403 Forbidden fetching markdownlint-cli2)*
- [x] `make -f Makefile.docs ssot`
- [ ] `make -f Makefile.docs links` *(markdown-link-check: No such file or directory)*

## Follow-ups
- add local tooling for markdownlint-cli2 and markdown-link-check
- investigate moving data/SCHEMA.md into dedicated docs section
