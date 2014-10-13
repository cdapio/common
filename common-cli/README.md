# Cask Common: CLI

**Introduction**

The CLI module contains a Command-Line Interface (CLI) framework for building your own CLI
complete with auto-completion and command-parsing.

## Usage

To use this CLI framework, you must first define a command:

```
public class EchoCommand implements Command {

  @Override
  public void execute(Arguments arguments, PrintStream output) throws Exception {
    output.println(arguments.get("some-input"));
  }

  @Override
  public String getPattern() {
    return "echo <some-input>";
  }

  @Override
  public String getDescription() {
    return "Echoes whatever is passed";
  }

}
```

This will define a command that would match the user input `echo something`, for example. In `execute()`,
`arguments.get("some-input")` would then return `something`, because `something` was matched to
`<some-input>` from the `getPattern()` implementation. You can then instantiate a `CLI` instance
and have it use your command:

```
public static void main(String[] args) throws IOException {
  CLI cli = new CLI<Command>(new EchoCommand());
  cli.run(args, System.out);
}
```

If args is empty, then the `CLI` will start in interactive mode. This will provide a shell (similar to bash)
from which the user can enter in multiple commands and use auto-completion.

If args is not empty, then the `CLI` will assume the args are the parts of a command, and execute the command.
In this example, if args is `["echo", "hello"]`, then the `CLI` will match the input to
the `EchoCommand` command and pass "hello" as the argument value for the "some-input" argument.

## Examples

* [HelloCLI](src/main/test/co/cask/cdap/common/cli/HelloCLI.java): the simple example presented above
* [NoteCLI](src/main/test/co/cask/cdap/common/cli/NoteCLI.java): notetaking CLI that demonstrates usage of multiple commands and completers

