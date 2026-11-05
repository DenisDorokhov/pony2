# Java style

- Use explicit types for local variables. Do not use `var`, including in tests and try-with-resources declarations.
- Place `@Nullable` immediately before the return type on the same line (for example, `public @Nullable String method()`), rather than above the method declaration.
- For multiline Java conditions, place logical operators (`&&`, `||`) at the end of the preceding line.
- Place private methods as close as possible to their first use.

# Application properties

- Use camelCase for application property names (for example, `pony.llm.maxCallsPerTool`).

# Comments

- Write all code comments, including JavaDoc, in English only.

# Artifacts outside the project

- Whenever you create an artifact outside the project (for example, an image, mockup, or document), always include an explicit clickable link to it in the final response, even if the tool already displays a preview.
- For local artifacts, also provide the full absolute path so the user can copy it and open the artifact manually. Do not rely on automatic previews or attachments in the JetBrains agent interface.

# Integration tests

- Reuse the shared Spring application context provided by `IntegrationTest`.
- Do not create additional Spring contexts with test-specific properties, profiles, configurations, `@TestPropertySource`, `@DynamicPropertySource`, `@MockitoBean`, `@MockitoSpyBean`, or `@DirtiesContext`.
- Use test-local collaborators when different configuration or mocks are needed. Restore any temporarily modified shared state after each test.

# Styles

- Prefer local component styles over changes in global stylesheets.
- Use global styles only for framework styles already customized in the global stylesheet, or when no local solution is available.
- Before adding a global rule, check whether the change can be implemented locally without bypassing style encapsulation.

# Browser automation

- Use only Playwright with isolated browser sessions for browser tasks. Do not use or control the user's desktop browser or personal browser profiles.
- If controlling a browser on the user's computer is necessary, first explain why Playwright is insufficient and ask for the user's permission. Proceed only after receiving explicit permission.

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
