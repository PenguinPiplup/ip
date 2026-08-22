# Project context

This repository is a starter template for a greenfield Java project used in an introductory software engineering course in an undergraduate computer science program. Students use it as the starting point for their own projects.

# Default user context

Unless the user says otherwise, assume that you are assisting a student working on a project in this repository.

# Student profile

* Prior knowledge: Basic Java and OOP concepts.
* Level of programming experience: Year 2 Computer Science student
* IDE and level of expertise: Some experience with IntelliJ and building full-stack applications

# Guidance for interacting with users

* Explain the rationale for significant actions: what you did and why.
* Keep explanations brief but instructive, supporting learning through responsible use of AI. For example:

  * When suggesting a Git command, briefly explain what it does.
  * Add explanatory Javadoc comments to all classes and to nontrivial methods and fields when their purpose or behavior is not obvious.
  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives.
  * When suggesting code, use the /present-changes-visually skill to compare the code with/without the code suggested by Claude.

# Project-specific requirements

* Do not make any changes to the code until the student has reviewed the code.

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## Testing

Whenever code under `src/` has been changed — whether Claude applied the change after the student reviewed it, or the student wrote it themselves — do both of the following before reporting the work as finished:

1. **Update `test/ui-test-plan.md` if the change affects the text UI.** Revise the expected output of every test case the change touches, and add a test case for behaviour that is new. Changes that are invisible from the console (renaming a private field, extracting a helper method) need no update to the plan.
2. **Invoke the `test-ui` skill** to run the plan, and show the student the console session it prints. The transcript is the evidence that the change actually works, so do not replace it with a summary.

A change is not finished until the test cases pass. When one fails, say whether the code or the plan is the thing that is out of date, and correct that one — never edit an expected output merely to make a failing test pass, because that turns a bug into the documented behaviour.

Keep the plan and the code in step in the same commit, so the repository never records a state where they disagree.

## Git

Use lightweight tags unless the user requests an annotated tag.
When proposing or creating a commit message, include enough detail to explain the rationale for the change.
Do not commit or push unless explicitly asked.
