# Session startup — mandatory

Conventions transversales chargées depuis `~/.claude/coding-conventions.md` (workflow par lot, commits, patterns interdits, ask-before-doing, secrets).

Before responding to any user request, run these silently :

1. Read `CLAUDE.md` — project-specific stack, architecture rules, secrets
2. Read `lots.md` — lot specifications and status
3. `git log --oneline -10`
4. `git status`
5. `git branch --show-current`
6. `./mvnw compile -q` — verify the project compiles (silent, report only if it fails)

Then summarize in exactly 3 lines :
- **Current lot**: which lot is active or next
- **State**: what is done, what is pending, any uncommitted work
- **Next action**: what you will do first

Do not start the user's request before completing this sequence.
