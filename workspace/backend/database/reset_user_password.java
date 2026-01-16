import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ResetPassword {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ResetPassword <email> <newPassword>");
            System.exit(1);
        }
        
        String email = args[0];
        String newPassword = args[1];
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(newPassword);
        
        System.out.println("UPDATE users SET password = '" + hashedPassword + "' WHERE email = '" + email + "';");
    }
}
