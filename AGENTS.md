# Java style

- Use explicit types for local variables. Do not use `var`, including in tests and try-with-resources declarations.
- Place `@Nullable` immediately before the return type on the same line (for example, `public @Nullable String method()`), rather than above the method declaration.
- For multiline Java conditions, place logical operators (`&&`, `||`) at the end of the preceding line.
- Place private methods as close as possible to their first use.

# Application properties

- Use camelCase for application property names (for example, `pony.llm.maxCallsPerTool`).
- Always prefer defining defaults directly in `@Value` expressions (for example, `@Value("${pony.llm.maxCallsPerTool:60}")`) over adding default values to `application.properties`.

# Comments

- Write all code comments, including JavaDoc, in English only.

# LLM prompt templates

- Use Apache Velocity (`.vm`) for LLM prompt templates.
- Render every application-authored system or user message from a complete template, including JSON-only payloads and verification or follow-up messages.
- Keep independently sent messages in separate files. Name system message templates `*-system.vm` and user message templates `*-user.vm`, including verification and follow-up messages.
- Prefix task-specific template filenames with the `DiscoveryTaskType` name in kebab-case, followed by a short step name when needed. Use `common-` for executor and compaction steps shared by task types.
- Give each logical LLM step its own message templates, even when their text currently duplicates another step. Do not share templates across different steps solely to eliminate duplication.
- Keep optional text inside conditional branches of the containing message template. Prefer self-contained prompt text over extracting shared fragments solely to eliminate duplication.

# Jackson annotations

- Do not add Jackson annotations without a concrete serialization or deserialization requirement that is not already covered by the existing global configuration and default behavior.
- Do not force null fields into JSON when clients already support omitted fields.

# External HTTP access in backend code

- These rules apply only to backend application code written or modified in this repository.
- Use direct connections for LLM API requests through the LLM SDK transport. Do not apply `pony.proxy.url` or route LLM requests through `ProxiedHttpClientFactory`. This includes chat completions and llama.cpp `/props`, `/apply-template`, and `/tokenize` requests.
- Access all other external HTTP resources through `ProxiedHttpClientFactory`, which reads `pony.proxy.url`. Do not fall back to direct connections for these resources.
- For non-LLM HTTP access, do not instantiate `OkHttpClient` or `OkHttpClient.Builder` directly outside `ProxiedHttpClientFactory` in production code.
- These rules do not apply to the agent's development tools, including Git, GitHub access, shell commands, and web browsing.
- For development tools, use their own network configuration. Do not apply `pony.proxy.url` or other application proxy settings to them.

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
