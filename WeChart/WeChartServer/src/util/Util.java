package util;

import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * 
 * @author LD
 *
 */
public class Util {
    
    /**
     * check str is number
     * @param str
     * @return
     */
    public static boolean checkIsNum(String str) {
        Pattern p = Pattern.compile("[0-9]*");
        return p.matcher(str).matches();
    }

    
    /**
     * JOptionPane show message
     * @param message
     * @param title
     * @param f
     * @param status
     */
    public static void showMessage(String message, String title, JFrame f, int status){
        JOptionPane.showMessageDialog(f, 
                                                                message, 
                                                                title, status
                                                                );
    }
}
