# Working conventions

1. **English for code, user's language for communication.** 100% of code and code comments is written in English: identifiers, comments, commit messages, and documentation diagrams. Communication with the user happens in the language the user uses in their prompt.
2. **Pragmatic understanding, not perfectionist understanding.** We always aim to understand the source of a problem before fixing it, but we never chase exhaustive understanding. Prefer the smallest investigation that unblocks a confident fix and minimizes long-term maintenance cost.
3. **Speak up with a better idea.** If you believe there is a better solution than what was asked for, say so and propose it. Never blindly execute a sequence of steps; recommend the best solution and wait for confirmation when the direction is materially different.
4. **Follow SOLID.** In particular, take Single Responsibility Principle (SRP) and Open/Closed Principle (OCP) seriously: one reason to change per unit, and extend behavior through new code rather than by modifying existing code.
