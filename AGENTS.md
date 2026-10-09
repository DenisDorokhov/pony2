# Java style

- Use explicit types for local variables. Do not use `var`, including in tests and try-with-resources declarations.

# Comments

- Write all code comments, including JavaDoc, in English only.

# Strict scope control

- Implement exactly what the user explicitly requests.
- Do not expand the scope based on inferred intentions.
- Do not refactor unrelated code.
- Do not make unsolicited improvements, cleanups, or optimizations.
- Do not introduce abstractions or architectural changes unless requested.
- Preserve existing behavior outside the requested change.
- If you identify additional problems, report them without fixing them.
- Prefer the smallest correct diff.
- Never modify unrelated files.
- Do not interpret an opportunity for improvement as authorization to act.