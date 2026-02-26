import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
        "/", 
        "/{x:[^\\.]*}", 
        "/**/{x:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}