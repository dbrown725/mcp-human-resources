package com.megacorp.humanresources.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {

 // creating a logger
 private static final Logger logger
     = LoggerFactory.getLogger(LogController.class);

 @RequestMapping("/log") public String log()
 {
     logger.debug("Entering log endpoint");
     // Logging various log level messages
     logger.trace("TRACE sample: fine-grained execution details for log endpoint");
     logger.debug("DEBUG sample: method-level diagnostic data");
     logger.info("INFO sample: log endpoint request processed");
     logger.warn("WARN sample: non-fatal condition for demonstration purposes");
     logger.error("ERROR sample: simulated serious issue for logging demonstration");

     return "Hey! You can check the output in the logs";
 }
}