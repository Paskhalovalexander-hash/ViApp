Follow ARCHITECTURE.md strictly.

Do NOT change architecture.
Do NOT add new layers.
Do NOT rename existing layers or concepts.
Do NOT move logic between layers.

You are allowed to implement ONLY the requested file
inside the existing architecture.

Context:
- UI uses Jetpack Compose
- ViewModel holds StateFlow only
- Orchestrator (Agent core) decides execution order
- Adapters execute actions
- Repositories handle data access
- DAO contains pure SQL only
- Database is Room
- Flow rules:
  - DAO returns Flow
  - Repository passes or maps Flow
  - ViewModel converts Flow → StateFlow
  - Adapters MUST NOT collect Flow


Requirements:
- Follow Kotlin and Android best practices
- Keep the implementation minimal and readable
- No business logic outside its layer
- No assumptions beyond provided architecture
- If something is unclear, choose the simplest valid solution
- Do NOT invent new abstractions

Output:
- Only code
- No explanations