package com.quorum.tessera.resteasy;

import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.ServerConfig;
import com.quorum.tessera.server.TesseraServer;
import com.quorum.tessera.server.TesseraServerFactory;

import javax.ws.rs.core.Application;
import java.util.stream.Stream;

/**
 * Creates RestEasy and Sun HTTP server implementations of the {@link RestServer}
 */
public class RestEasyServerFactory implements TesseraServerFactory {

    @Override
    public TesseraServer createServer(ServerConfig serverConfig,Object... args) {
        Application application = Stream.of(args).findFirst()
                .map(Application.class::cast)
                .get();
        
        return new RestEasyServer(application, serverConfig);
    }

    @Override
    public CommunicationType communicationType() {
        return CommunicationType.REST;
    }


    
}
