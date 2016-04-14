package info.krumholz.tcgservice.configuration;

import java.util.Properties;

import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.server.SoapMessageDispatcher;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.transport.http.WsdlDefinitionHandlerAdapter;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import info.krumholz.tcgservice.interfaces.soap.ExceptionHandler;

@Configuration
@EnableWs
@ComponentScan(basePackages = { "info.krumholz.tcgservice.ws" })
public class ServiceConfiguration extends WsConfigurerAdapter {

	@Bean
	public SoapFaultMappingExceptionResolver exceptionResolver() {
		ExceptionHandler exceptionHandler = new ExceptionHandler();
		exceptionHandler.setOrder(1);
		SoapFaultDefinition defaultFault = new SoapFaultDefinition();
		// TODO: provide better fault message
		defaultFault.setFaultStringOrReason("MUHAHAHAHHAHA");
		exceptionHandler.setDefaultFault(defaultFault);
		return exceptionHandler;
	}

	@Bean
	public SaajSoapMessageFactory messageFactory() {
		SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory();
		messageFactory.setSoapVersion(SoapVersion.SOAP_11);
		return messageFactory;
	}

	@Bean
	public SoapMessageDispatcher messageDispatcher() {
		SoapMessageDispatcher messageDispatcher = new SoapMessageDispatcher();
		return messageDispatcher;
	}

	@Bean
	public WsdlDefinitionHandlerAdapter wsdlDefinitionHandlerAdapter() {
		WsdlDefinitionHandlerAdapter adapter = new WsdlDefinitionHandlerAdapter();
		adapter.setTransformLocations(true);
		return adapter;
	}

	@Bean(name = "service")
	public DefaultWsdl11Definition wsdlDefinition(XsdSchema cardServiceSchema) {
		DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
		wsdl11Definition.setPortTypeName("ServicePort");
		wsdl11Definition.setLocationUri("/ws");
		wsdl11Definition.setTargetNamespace("http://krumholz.info/tcgservice");
		wsdl11Definition.setSchema(cardServiceSchema);
		wsdl11Definition.setServiceName("tcgservice");
		return wsdl11Definition;
	}

	@Bean
	public SimpleUrlHandlerMapping webserviceUrlMapping() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setDefaultHandler(null);
		Properties p = new Properties();
		p.setProperty("/ws/*", "messageDispatcher");
		p.setProperty("/ws/service.wsdl", "service");
		mapping.setMappings(p);
		return mapping;
	}

	@Bean(name = "dispatcher")
	public ServletRegistrationBean dispatcherServlet(ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean(servlet, "/ws/*");
	}

	@Bean
	public XsdSchema cardServiceSchema() {
		ClassPathResource xsdResource = new ClassPathResource("service.xsd");
		SimpleXsdSchema cardServiceSchema = new SimpleXsdSchema(xsdResource);
		return cardServiceSchema;
	}
}
