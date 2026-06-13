# AGENTS.md

This file provides guidance for AI coding agents when working with code in this repository.

## Project Rules

Full project rules (tech stack, coding standards, module structure) are in:
- [CLAUDE.md](./CLAUDE.md) — entry point
- [.claude/project-rules.md](./.claude/project-rules.md) — detailed rules

## Available Skills

Skill docs are in `.agent/skills/` (mirrors `.claude/skills/`, kept in sync):

| Skill | When to use |
|---|---|
| [backend_development](./.agent/skills/backend_development/SKILL.md) | Implementing a new feature or entity (CRUD, validation, converter, DDL) |
| [unit_testing](./.agent/skills/unit_testing/SKILL.md) | Writing or reviewing unit/integration tests |
