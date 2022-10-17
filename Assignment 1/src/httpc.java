import java.util.Scanner;

public class httpc {
  public static void main(String[] args) throws Exception {
    clientLibrary clientLibrary = new clientLibrary();

    System.out.println("Please input your request in the terminal ---->");

    Scanner inputStream = new Scanner(System.in);
    String input = inputStream.nextLine();

    if (!input.isEmpty() && input.contains("httpc")) {
      if (!input.contains("help")) {
        if (input.contains("get")) {
          System.out.println(clientLibrary.GET(input));
        } else if (input.contains("post")) {
          System.out.println(clientLibrary.POST(input));
        } else {
          System.out.println("Your input is not valid.");
          System.exit(0);
        }
        // help commands
      } else {
        help(input);
      }
    } else {
      System.out.println("Your input is not valid.");
      System.exit(0);
    }
  }

  public static void help(String string) {
    if (string.contains("get")) {
      System.out.println("usage: httpc get [-v] [-h key:value] URL");
      System.out.println("Get executes a HTTP GET request for a given URL.");
      System.out.println("    -v  Prints the detail of the response such as protocol, status, and requestHeaders.");
      System.out.println("    -h  key:value Associates requestHeaders to HTTP Request with the format 'key:value'.");
    }

    else if (string.contains("post")) {
      System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL");
      System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.");
      System.out.println("    -v  Prints the detail of the response such as protocol, status, and requestHeaders.");
      System.out.println("    -h  key:value Associates requestHeaders to HTTP Request with the format 'key:value'.");
      System.out.println("    -d  string Associates an inline data to the body HTTP POST request.");
      System.out.println("    -f  file Associates the content of a file to the body HTTP POST request.");
      System.out.println("Either [-d] or [-f] can be used but not both.");
    }

    else {
      System.out.println("httpc is a curl-like application but supports HTTP protocol only.");
      System.out.println("Usage:");
      System.out.println("    httpc command [arguments]");
      System.out.println("The commands are:");
      System.out.println("    get     executes a HTTP GET request and prints the response.");
      System.out.println("    post    executes a HTTP POST request and prints the response.");
      System.out.println("    help    prints this screen.");
      System.out.println();
      System.out.println("Use \"httpc help [command]\" for more information about a command.");
    }
  }
}

// TESTs
// httpc help
// httpc help get
// httpc help post
// httpc get 'http://httpbin.org/get?course=networking&assignment=1'
// httpc get -v 'http://httpbin.org/get?course=networking&assignment=1'
// inline -d post: httpc post -h Content-Type:application/json --d '{"Asshignment": 1}' 'http://httpbin.org/post'
// file -f post: httpc post -h Content-Type:application/json --f bodyFile.txt 'http://httpbin.org/post'
// [bonus 2]test -o to write response to hello.txt: httpc -v 'http://httpbin.org/get?course=networking&assignment=1' -o hello.txt
