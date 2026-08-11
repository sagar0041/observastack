# Contributing

## Branches

`main` is always deployable. Work happens on short-lived branches:

```
feat/order-placement
fix/inventory-double-reserve
chore/bump-otel-agent
docs/adr-kafka-choice
```

## Commits

Conventional Commits, imperative mood, lowercase subject, no trailing
period. Subject line under 72 characters.

```
feat(order): add order placement use case
fix(inventory): release reservation when payment fails
refactor(order): extract pricing into value object
test(inventory): cover concurrent reservation conflict
chore(build): pin testcontainers to 1.20.4
docs(adr): record decision to use liquibase over flyway
```

Body only when the *why* isn't obvious from the subject. Wrap at 72
characters. Explain reasoning and trade-offs, not a list of changed files —
the diff already says what changed.

```
fix(inventory): release reservation when payment fails

Reservations were held until the 30 minute TTL expired even when payment
had already been declined, which blocked stock that was never going to
sell. Listening for PaymentDeclined releases immediately.

The TTL sweep stays as a backstop for the case where the payment event
is lost entirely.
```

Avoid: multi-bullet summaries of every file touched, marketing adjectives
("robust", "comprehensive", "seamless"), and commits that bundle unrelated
changes.

## Pull requests

- One milestone or one logical change per PR.
- Description states what changed and why, plus anything the reviewer
  should check manually.
- CI must be green before merge.
- Squash on merge; the squash message follows the commit rules above.
