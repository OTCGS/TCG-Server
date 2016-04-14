package info.krumholz.tcgservice.configuration;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import info.krumholz.tcgservice.Main;

//@EnableWebMvc
@Configuration
@ComponentScan(basePackages = { "info.krumholz.tcgservice.web" })
public class WebConfiguration extends WebMvcConfigurerAdapter {

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(RSAPublicKey.class, new JsonSerializer<RSAPublicKey>() {

			@Override
			public void serialize(RSAPublicKey value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonProcessingException {
				jgen.writeStartObject();
				jgen.writeFieldName("publicKey");
				jgen.writeString(Base64.getEncoder().encodeToString(value.getEncoded()));
				jgen.writeEndObject();
			}

		});
		objectMapper.registerModule(module);
		converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/resources/**").addResourceLocations("classpath:www/");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
		localeChangeInterceptor.setParamName("lang");
		registry.addInterceptor(localeChangeInterceptor);
	}

	@Bean
	public LocaleResolver localeResolver() {
		CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
		cookieLocaleResolver.setDefaultLocale(StringUtils.parseLocaleString("en"));
		return cookieLocaleResolver;
	}

	@Bean
	public ThymeleafViewResolver thymeleafViewResolver() {
		return new ThymeleafViewResolver();
	}

	@Bean
	public SpringResourceTemplateResolver templateResolver() {
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
		resolver.setPrefix("classpath:templates/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode("HTML5");
		resolver.setCacheable(false);
		return resolver;
	}

	@Bean
	public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver templateResolver) {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setTemplateResolver(templateResolver);
		return engine;
	}

	@Bean
	public ViewResolver viewResolver(SpringTemplateEngine templateEngine) {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setTemplateEngine(templateEngine);
		viewResolver.setOrder(1);
		viewResolver.setViewNames(new String[] { "*" });
		viewResolver.setCache(false);
		return viewResolver;
	}

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("classpath:messages/messages", "classpath:messages/validation");
		messageSource.setUseCodeAsDefaultMessage(true);
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setCacheSeconds(0);
		return messageSource;

	}

	@Bean(name = "multipartResolver")
	public MultipartResolver multipartResolver() {
		StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
		return resolver;
	}

	@Bean
	public MultipartFilter multipartFilter() {
		MultipartFilter multipartFilter = new MultipartFilter();
		multipartFilter.setMultipartResolverBeanName("multipartResolver");
		return multipartFilter;
	}

	@Bean
	public EmbeddedServletContainerCustomizer customizer(ApplicationContext context) {
		return new EmbeddedServletContainerCustomizer() {

			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
				if (container instanceof JettyEmbeddedServletContainerFactory) {
					customizeJetty((JettyEmbeddedServletContainerFactory) container);
				}
			}

			private void customizeJetty(JettyEmbeddedServletContainerFactory jetty) {
				jetty.addServerCustomizers(new JettyServerCustomizer() {

					@Override
					public void customize(Server server) {
						HttpConfiguration config = new HttpConfiguration();
						if (Main.ssl) {
							config.setSecurePort(443);
							config.setSecureScheme("https");
						}
						config.addCustomizer(new SecureRequestCustomizer());
						ServerConnector notSsl = new ServerConnector(server);
						notSsl.addConnectionFactory(new HttpConnectionFactory(config));
						notSsl.setPort(jetty.getPort());

						ServerConnector sslConnector = null;
						if (Main.ssl) {
							SslContextFactory sslContextFactory = new SslContextFactory(
									getClass().getResource("/keystore.jks").toExternalForm());
							sslContextFactory.setKeyStorePassword("tcg$login");
							sslContextFactory.setKeyManagerPassword("tcg$login");

							HttpConfiguration sslConfig = new HttpConfiguration();
							sslConfig.addCustomizer(new SecureRequestCustomizer());

							sslConnector = new ServerConnector(server, sslContextFactory);
							sslConnector.setPort(443);

							// setup the constraint that causes all http
							// requests to
							// return a !403 error
							ConstraintSecurityHandler security = new ConstraintSecurityHandler();
							security.setHandler(server.getHandler());

							Constraint constraint = new Constraint();
							constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);

							// makes the constraint apply to all uri paths
							ConstraintMapping mapping = new ConstraintMapping();
							mapping.setPathSpec("/*");
							mapping.setConstraint(constraint);

							security.addConstraintMapping(mapping);
							server.setHandler(security);
						}
						server.setConnectors(sslConnector == null ? new Connector[] { notSsl }
								: new Connector[] { sslConnector, notSsl });
					}
				});
			}
		};

	}
}
