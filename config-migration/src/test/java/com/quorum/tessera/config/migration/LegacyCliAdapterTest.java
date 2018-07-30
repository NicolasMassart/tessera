package com.quorum.tessera.config.migration;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.Peer;
import com.quorum.tessera.config.SslTrustMode;
import com.quorum.tessera.config.builder.ConfigBuilder;
import com.quorum.tessera.config.cli.CliResult;
import com.quorum.tessera.config.migration.test.FixtureUtil;
import com.quorum.tessera.test.util.ElUtil;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LegacyCliAdapterTest {

    private final ConfigBuilder builderWithValidValues = FixtureUtil.builderWithValidValues();

    private final LegacyCliAdapter instance = new LegacyCliAdapter();

    @Test
    public void help() throws Exception {

        CliResult result = instance.execute("help");
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).isNotPresent();
        assertThat(result.getStatus()).isEqualTo(0);

    }

        @Test
    public void noArgs() throws Exception {

        CliResult result = instance.execute();
        assertThat(result).isNotNull();
        assertThat(result.getConfig()).isNotPresent();
        assertThat(result.getStatus()).isEqualTo(1);

    }
    
    @Test
    public void noOptionsWithTomlFile() throws Exception {

        Path serverKeyStorePath = Files.createTempFile("serverKeyStorePath", ".bog");
        Path passwordFile = Files.createTempFile("passwords", ".txt");
        Files.write(passwordFile, Arrays.asList("PASWORD1"));

        Path sampleFile = Paths.get(getClass().getResource("/sample-all-values.conf").toURI());
        Map<String, Object> params = new HashMap<>();
        params.put("passwordFile", passwordFile);
        params.put("serverKeyStorePath", serverKeyStorePath);
        String data = ElUtil.process(Files.readAllLines(sampleFile)
                .stream()
                .collect(Collectors.joining(System.lineSeparator())), params);

        Path configFile = Files.createTempFile("noOptions", ".txt");
        Files.write(configFile, data.getBytes());

        CliResult result = instance.execute(configFile.toString());

        assertThat(result).isNotNull();
        assertThat(result.getConfig()).isPresent();
        assertThat(result.getStatus()).isEqualTo(0);

        Files.deleteIfExists(configFile);
        Files.deleteIfExists(passwordFile);
        Files.deleteIfExists(serverKeyStorePath);
    }

    @Test
    public void applyOverrides() throws Exception {

        String urlOverride = "http://junit.com:8989";
        int portOverride = 9999;
        String unixSocketFileOverride = "unixSocketFileOverride.ipc";
        String workdirOverride = "workdirOverride";
        List<Peer> overridePeers = Arrays.asList(new Peer("http://otherone.com:9188/other"), new Peer("http://yetanother.com:8829/other"));

        CommandLine commandLine = mock(CommandLine.class);

        when(commandLine.getOptionValue("url")).thenReturn(urlOverride);
        when(commandLine.getOptionValue("port")).thenReturn(String.valueOf(portOverride));
        when(commandLine.getOptionValue("socket")).thenReturn(unixSocketFileOverride);
        when(commandLine.getOptionValue("workdir")).thenReturn(workdirOverride);
        
        when(commandLine.getOptionValues("othernodes"))
                .thenReturn(
                        overridePeers.stream().map(Peer::getUrl).toArray(String[]::new)
                );

        when(commandLine.getOptionValue("storage")).thenReturn("sqlite:somepath");

        when(commandLine.getOptionValue("tlsservertrust")).thenReturn("whitelist");

        when(commandLine.getOptionValue("tlsclienttrust")).thenReturn("ca");

        when(commandLine.getOptionValue("tlsservercert")).thenReturn("tlsservercert.cert");

        when(commandLine.getOptionValue("tlsclientcert")).thenReturn("tlsclientcert.cert");

        when(commandLine.getOptionValues("tlsserverchain")).thenReturn(new String[]{
            "server1.crt", "server2.crt", "server3.crt"
        });

        when(commandLine.getOptionValues("tlsclientchain")).thenReturn(new String[]{
            "client1.crt", "client2.crt", "client3.crt"
        });

        when(commandLine.getOptionValue("tlsserverkey"))
                .thenReturn("tlsserverkey.key");

        when(commandLine.getOptionValue("tlsclientkey"))
                .thenReturn("tlsclientkey.key");

        when(commandLine.getOptionValues("publickeys"))
                .thenReturn(new String[]{"ONE", "TWO"});

        doReturn(new String[]{}).when(commandLine).getOptionValues("alwayssendto");

        List<Path> privateKeyPaths = Arrays.asList(
                Files.createTempFile("applyOverrides1", ".txt"),
                Files.createTempFile("applyOverrides2", ".txt")
        );

        when(commandLine.getOptionValue("tlsknownservers")).thenReturn("tlsknownservers.file");

        when(commandLine.getOptionValue("tlsknownclients")).thenReturn("tlsknownclients.file");

        final byte[] privateKeyData = FixtureUtil.createLockedPrivateKey().toString().getBytes();
        for (Path p : privateKeyPaths) {
            Files.write(p, privateKeyData);
        }

        final String[] privateKeyPathStrings = privateKeyPaths
                .stream()
                .map(Path::toString)
                .toArray(String[]::new);

        when(commandLine.getOptionValues("privatekeys")).thenReturn(privateKeyPathStrings);

        final List<String> privateKeyPasswords = Arrays.asList("SECRET1", "SECRET2");

        final Path privateKeyPasswordFile = Files.createTempFile("applyOverridesPasswords", ".txt");
        Files.write(privateKeyPasswordFile, privateKeyPasswords);

        when(commandLine.getOptionValue("passwords"))
                .thenReturn(privateKeyPasswordFile.toString());

        Config result = LegacyCliAdapter.applyOverrides(commandLine, builderWithValidValues).build();

        assertThat(result).isNotNull();
        assertThat(result.getServerConfig().getHostName()).isEqualTo(urlOverride);
        assertThat(result.getServerConfig().getPort()).isEqualTo(portOverride);
        assertThat(result.getUnixSocketFile()).isEqualTo(Paths.get(workdirOverride,unixSocketFileOverride));
        assertThat(result.getPeers()).containsExactly(overridePeers.toArray(new Peer[0]));
        assertThat(result.getKeys().getKeyData()).hasSize(2);
        assertThat(result.getJdbcConfig()).isNotNull();
        assertThat(result.getJdbcConfig().getUrl()).isEqualTo("jdbc:sqlite:somepath");

        assertThat(result.getServerConfig().getSslConfig().getServerTrustMode()).isEqualTo(SslTrustMode.WHITELIST);
        assertThat(result.getServerConfig().getSslConfig().getClientTrustMode()).isEqualTo(SslTrustMode.CA);

        assertThat(result.getServerConfig().getSslConfig().getClientTlsCertificatePath()).isEqualTo(Paths.get("tlsclientcert.cert"));

        assertThat(result.getServerConfig().getSslConfig().getServerTlsCertificatePath()).isEqualTo(Paths.get("tlsservercert.cert"));

        assertThat(result.getServerConfig().getSslConfig().getServerTrustCertificates())
                .containsExactly(Paths.get("server1.crt"), Paths.get("server2.crt"), Paths.get("server3.crt"));

        assertThat(result.getServerConfig().getSslConfig().getClientTrustCertificates())
                .containsExactly(Paths.get("client1.crt"), Paths.get("client2.crt"), Paths.get("client3.crt"));

        assertThat(result.getServerConfig().getSslConfig().getServerKeyStore())
                .isEqualTo(Paths.get("tlsserverkey.key"));

        assertThat(result.getServerConfig().getSslConfig().getClientKeyStore())
                .isEqualTo(Paths.get("tlsclientkey.key"));

        assertThat(result.getServerConfig().getSslConfig().getKnownServersFile())
                .isEqualTo(Paths.get("tlsknownservers.file"));

        assertThat(result.getServerConfig().getSslConfig().getKnownClientsFile())
                .isEqualTo(Paths.get("tlsknownclients.file"));

        Files.deleteIfExists(privateKeyPasswordFile);
        for (Path privateKeyPath : privateKeyPaths) {
            Files.deleteIfExists(privateKeyPath);
        }

    }

    @Test
    public void applyOverridesNullValues() {

        Config expectedValues = builderWithValidValues.build();

        CommandLine commandLine = mock(CommandLine.class);

        Config result = LegacyCliAdapter.applyOverrides(commandLine, builderWithValidValues).build();

        assertThat(result).isNotNull();

        assertThat(result.getServerConfig().getHostName())
                .isEqualTo(expectedValues.getServerConfig().getHostName());

        assertThat(result.getServerConfig().getPort())
                .isEqualTo(expectedValues.getServerConfig().getPort());

        assertThat(result.getUnixSocketFile())
                .isEqualTo(expectedValues.getUnixSocketFile());

        assertThat(result.getPeers())
                .containsOnlyElementsOf(expectedValues.getPeers());

        assertThat(result.getJdbcConfig().getUrl()).isEqualTo("jdbc:bogus");

        assertThat(result.getServerConfig().getSslConfig().getServerTrustMode()).isEqualTo(SslTrustMode.TOFU);

        assertThat(result.getServerConfig().getSslConfig().getClientTrustMode()).isEqualTo(SslTrustMode.CA_OR_TOFU);

        assertThat(result.getServerConfig().getSslConfig().getClientKeyStore()).isEqualTo(Paths.get("sslClientKeyStorePath"));

        assertThat(result.getServerConfig().getSslConfig().getServerKeyStore()).isEqualTo(Paths.get("sslServerKeyStorePath"));

        assertThat(result.getServerConfig().getSslConfig().getServerTrustCertificates()).containsExactly(Paths.get("sslServerTrustCertificates"));

        assertThat(result.getServerConfig().getSslConfig().getClientTrustCertificates()).containsExactly(Paths.get("sslClientTrustCertificates"));

        assertThat(result.getServerConfig().getSslConfig().getKnownServersFile())
                .isEqualTo(Paths.get("knownServersFile"));

        assertThat(result.getServerConfig().getSslConfig().getKnownClientsFile())
                .isEqualTo(Paths.get("knownClientsFile"));

    }

    @Test
    public void resolveUnixFilePathInitalValueOnly() {
        Path relativePath = Paths.get("someopath.ipc");

        Optional<Path> result = LegacyCliAdapter.resolveUnixFilePath(relativePath, null, null);

        assertThat(result).isPresent().get().isEqualTo(relativePath);

    }

    @Test
    public void resolveUnixFilePathAllNull() {

        Optional<Path> result = LegacyCliAdapter.resolveUnixFilePath(null, null, null);

        assertThat(result).isNotPresent();

    }


    @Test
    public void resolveUnixFilePathWorkdirOnly() {

        Optional<Path> result = LegacyCliAdapter.resolveUnixFilePath(null, "dir", null);

        assertThat(result).isNotPresent();

    }

    @Test
    public void resolveUnixFilePathWorkdirAndFileName() {

        Optional<Path> result = LegacyCliAdapter.resolveUnixFilePath(null, "dir", "somefile.file");

        assertThat(result).isPresent().get().isEqualTo(Paths.get("dir", "somefile.file"));

    }



    @Test
    public void writeToOutputFileValidationError() throws Exception {

        Config config = mock(Config.class);
        
        Path outputPath = Files.createTempFile("writeToOutputFileValidationError", ".txt");
        
        CliResult result =  LegacyCliAdapter.writeToOutputFile(config, outputPath);
        
        assertThat(result.getStatus()).isEqualTo(2);
        
        Files.deleteIfExists(outputPath);
    }
 
}