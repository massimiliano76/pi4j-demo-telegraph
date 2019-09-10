package com.pi4j.demo.telegraph;
/*-
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Pi4J
 * PROJECT       :  Pi4J :: DEMO  :: Telegraph Demo
 * FILENAME      :  TelegraphUsingDI.java
 *
 * This file is part of the Pi4J project. More information about
 * this project can be found here:  https://pi4j.com/
 * **********************************************************************
 * %%
 * Copyright (C) 2012 - 2019 Pi4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.pi4j.Pi4J;
import com.pi4j.annotation.Inject;
import com.pi4j.annotation.OnEvent;
import com.pi4j.context.Context;
import com.pi4j.event.InitializedEvent;
import com.pi4j.event.ShutdownEvent;
import com.pi4j.io.binding.OnOffBinding;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.pwm.Pwm;

import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * <h2>TelegraphUsingAnnotatedDI Sample</h2>
 * <p>This example utilizes the new Pi4J annotation framework to perform runtime dependency injection
 * to wire up and configure the I/O interfaces with Pi4J.  This is a very declarative style/approach to using
 * to using the Pi4J APIs.</p>
 * <p>This project is available on <a href="https://github.com/Pi4J/pi4j-demo-telegraph">GitHub</a></p>
 *
 * @author Robert Savage (<a href="http://www.savagehomeautomation.com">http://www.savagehomeautomation.com</a>)
 * @version $Id: $Id
 */
public class TelegraphUsingAnnotatedAI {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     * @throws Exception if any.
     */
    public static void main(String[] args) throws Exception {
        // configure default lolling level, accept a log level as the fist program argument
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");

        // instruct PIGPIO which remote Raspberry Pi to connect to
        //System.setProperty("pi4j.host", "127.0.0.1");
        //System.setProperty("pi4j.pigpio.remote", "false");

        // get properties files/stream from embedded resource file
        InputStream propertiesStream = TelegraphUsingAnnotatedAI.class.getClassLoader()
                .getResourceAsStream("pi4j.properties");

        // Pi4J cannot perform dependency injection on static classes
        // we will create a container instance to run our example
        RuntimeContainer container = new TelegraphUsingAnnotatedAI.RuntimeContainer();

        // Initialize Pi4J with an auto context and load properties into context
        Pi4J.newContextBuilder()
            .autoDetect()
            .autoInject()  // <--THIS WILL ATTEMPT TO AUTO REGISTER AND INJECT I/O
                           //    INSTANCES FROM PROPERTIES INTO Pi4J CONTEXT/REGISTRY
            .properties(propertiesStream)
            .build().inject(container);  // <-- INJECT INTO THE CONTAINER CLASS HERE

        // load the container class now
        container.call();
    }

    public static class RuntimeContainer implements Callable<Void> {

        @Inject
        Context pi4j;

        @Inject("sounder")     // <-- NOTICE THAT WE ARE INJECTING EXISTING I/O INSTANCES, NOT REGISTERING THEM
        private DigitalOutput sounder;

        @Inject("led")
        private DigitalOutput led;

        @Inject("left-audio-channel")
        private Pwm left;

        @Inject("right-audio-channel")
        private Pwm right;

        @Inject()
        private DigitalInput key;

        // setup a digital input event listener to listen for any value changes on the digital input
        // using a custom method with a single event parameter
        @OnEvent("key")
        private void onDigitalInputChange(DigitalStateChangeEvent event){
            System.out.println("TELEGRAPH DEMO :: " + event);
        }

        @OnEvent
        private void onShutdown(ShutdownEvent event){
            System.out.println("---------------------------------------------------");
            System.out.println("[Pi4J V.2 DEMO] SHUTTING DOWN");
            System.out.println("---------------------------------------------------");
        }

        @OnEvent
        private void onInitialized(InitializedEvent event){
            pi4j.registry().describe().print(System.out);
            System.out.println("---------------------------------------------------");
            System.out.println(" Press the telegraph key when ready.");
        }

        @Override
        public Void call() throws Exception {

            // bind the input changes from the Telegraph Key to the various outputs
            key.bind(OnOffBinding.newInstance(sounder, led, left, right));

            System.out.println("---------------------------------------------------");
            System.out.println(" [Pi4J V.2 DEMO] TELEGRAPH (Using DI/Annotations)");
            System.out.println("---------------------------------------------------");

            // keep the program running until we see user input
            System.in.read();

            // shutdown Pi4J context now
            pi4j.shutdown();

            return null;
        }
    }
}
