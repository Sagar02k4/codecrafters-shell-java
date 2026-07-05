import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        // implemented type builtin
        // need to implement
        // pwd
        // cd
        // mkdir
        // cat
        // export
        // which
        // if statement
        // exit status
        
        while(true){
            System.out.print("$ ");
            String input = sc.nextLine();

            String command = input.indexOf(" ") == -1 ? input : input.substring(0, input.indexOf(" "));
            String remainder = input.indexOf(" ") == -1 ? "" : input.substring(input.indexOf(" ") + 1);
            
            if(command.equals("echo")){
                System.out.println(remainder);
            }
            else if(command.equals("exit")){
                break;
            }
            else if(command.equals("type")){
                if(remainder.equals("echo") || remainder.equals("exit") || remainder.equals("type")){
                    System.out.println(remainder + " is a shell builtin");
                }
                else{
                    System.out.println(remainder + ": not found");
                }
            }
            else{
                System.out.println(command + ": command not found");
            }
        }
        System.out.println("exit");

    }
}
