package it.eng.parer.eidas.web.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import it.eng.parer.eidas.core.helper.ViewHelper;
import it.eng.parer.eidas.core.util.Constants;

@Controller
public class WelcomeController {

    @Autowired
    Environment env;

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    ViewHelper viewHelper;

    @GetMapping("/")
    public ModelAndView main(Model model, SessionStatus status) {

        build(model);

        status.setComplete();
        return new ModelAndView("index");
    }

    @GetMapping("/admin")
    public ModelAndView admin(Model model, SessionStatus status) {

        build(model);

        status.setComplete();
        return new ModelAndView("admin");
    }

    private void build(Model model) {
        // application properties
        viewHelper.convertAppPropertiesAsMap(model);
        // app infos
        this.infos(model);
    }

    private void infos(Model model) {
        model.addAttribute("version", env.getProperty(Constants.BUILD_VERSION));
        model.addAttribute("builddate", env.getProperty(Constants.BUILD_TIME));
        model.addAttribute("dss", buildProperties.get(Constants.DSS_VERSION));
    }

}
