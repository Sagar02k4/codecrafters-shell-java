import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        
        while(true){
            System.out.print("$ ");
            String input = sc.nextLine();

            String command = input.indexOf(" ") == -1 ? input : input.substring(0, input.indexOf(" "));
            String remainder = input.indexOf(" ") == -1 ? "" : input.substring(input.indexOf(" ") + 1);
            if(command.startsWith("echo ")){
                System.out.println(command.substring(5));
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
                System.out.println(command + ": not found");
            }
        }
    }
}
