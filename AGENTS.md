# Java style

- Use explicit types for local variables. Do not use `var`, including in tests and try-with-resources declarations.
- Place `@Nullable` immediately before the return type on the same line (for example, `public @Nullable String method()`), rather than above the method declaration.
- For multiline Java conditions, place logical operators (`&&`, `||`) at the end of the preceding line.
- Place private methods as close as possible to their first use.

# Comments

- Write all code comments, including JavaDoc, in English only.

# Styles

- Prefer local component styles over changes in global stylesheets.
- Use global styles only for framework styles already customized in the global stylesheet, or when no local solution is available.
- Before adding a global rule, check whether the change can be implemented locally without bypassing style encapsulation.

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
