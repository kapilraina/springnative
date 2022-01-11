package com.example.reactivespringnative;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.nativex.hint.AotProxyHint;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ProxyBits;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.SerializationHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Use the BeanFactoryNativeConfigurationProcessor, or
 * BeanNativeConfigurationProcessor Or
 * NativeConfiguration
 * https://spring.io/blog/2021/12/09/new-aot-engine-brings-spring-native-to-the-next-level
 */

@NativeHint(

		trigger = AutoConfigurations.class, // Hints are evaluated if the trigger is valid
		// Command Line Switches
		// options = { "--enable-https", "--somecustom arg" }, // GraalVM Native Options
		resources = @ResourceHint(patterns = { "data/csvresource.csv" }), // Hint to Load Resources in Classpath
		serializables = @SerializationHint(types = { Subject.class, String.class, Integer.class, Number.class }),

		types = @TypeHint(types = {// Reflection
				SimpleSubjectService.class
		}, access = {
				TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_METHODS,
				TypeAccess.PUBLIC_CONSTRUCTORS,
				TypeAccess.PUBLIC_METHODS
		}), jdkProxies ={ 
			@JdkProxyHint(
				types = {
				com.example.reactivespringnative.CustomerService.class
				}),
			@JdkProxyHint(
				types = {
					com.example.reactivespringnative.SubjectService.class,
					org.springframework.aop.SpringProxy.class,
					org.springframework.aop.framework.Advised.class,
					org.springframework.core.DecoratingProxy.class
				})

		}, aotProxies = @AotProxyHint(targetClass = com.example.reactivespringnative.StandaloneSubjectService.class, proxyFeatures = ProxyBits.IS_STATIC)

)

@SpringBootApplication
@RequiredArgsConstructor
public class ReactiveSpringNativeApplication {

	private final Subjectrepo repo;
	Logger log = LoggerFactory.getLogger(ReactiveSpringNativeApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ReactiveSpringNativeApplication.class, args);
	}

	@Bean
	public ApplicationRunner reflectiveRunner() {
		return args -> {

			try {
				String classname = "com.example.reactivespringnative.SimpleSubjectService";
				Class<?> clazz = Class.forName(classname);
				Constructor<?>[] clazzConstructors = clazz.getDeclaredConstructors();
				log.info("\n \n Declared Constructors in class SimpleSubjectService : " + clazzConstructors.length);
				if (clazzConstructors.length == 0) {
					clazzConstructors = clazz.getConstructors();
					log.info("\n \n Constructors in class SimpleSubjectService : " + clazzConstructors.length);
				}
				if (clazzConstructors.length > 0) {
					Object instance = clazz.getDeclaredConstructors()[0].newInstance();
					Method method = clazz.getDeclaredMethod("findFirstHiddenSubject");
					Object sub = (Subject) method.invoke(instance);
					log.info("\n Hidden Subject via reflection : " + sub);
				} else {
					log.error("\n No constructors could be retrieved for SimpleSubjectService");
				}

			} catch (Exception e) {
				log.error("Error in reflectiveRunner  " + e);
			}

		};
	}

	@Bean
	public ApplicationRunner custalr(CustomerService cs) {
		return args -> {
			log.info("\n \n Fantom Customer via JDK Proxy :" +
					cs.createDefaultFantomCustomer(0));
			System.out.println("\n CustomerService Interfaces \n");
			Stream.of(cs.getClass().getInterfaces()).forEach(
					i -> System.out.println(i.getName()));

		};
	}

	@Bean
	public ApplicationRunner aotclassproxyrunner(StandaloneSubjectService sss) {
		return args -> {
			sss.findFirstHiddenSubject();
			log.info("\n \n Top Hidden Subject : " + sss.findTopHiddenSubject());

			System.out.println("\n StandaloneSubjectService Interfaces \n");
			Stream.of(sss.getClass().getInterfaces()).forEach(
					i -> System.out.println(i.getName()));

		};
	}

	@Bean
	public ApplicationRunner aotinterfaceproxyrunner(SubjectService ss) {
		return args -> {
			log.info("\n \n First Hidden Subject : " +
					ss.findFirstHiddenSubject());

			System.out.println("\n SubjectService Interfaces \n");
			Stream.of(ss.getClass().getInterfaces()).forEach(
					i -> System.out.println(i.getName()));

		};
	}

	// JDK Proxy
	@Bean
	CustomerService customerService() {
		try {
			InvocationHandler ih = new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					if (method.getName().equals("createDefaultFantomCustomer")) {
						return new Customer((Integer) args[0], "Fantom C", "Nowhere");
					}
					return null;
				}

			};
			ClassLoader classLoader = ClassLoader.getSystemClassLoader();
			Class<?> clazz = Class.forName("com.example.reactivespringnative.CustomerService");
			Object proxy = Proxy.newProxyInstance(classLoader, new Class[] { clazz }, ih);
			return (CustomerService) proxy;
		} catch (Exception e) {
			log.error("Error in customerService Proxy Creator  " + e);
		}

		return null;

	}

	// AOT Class Proxy
	@Bean
	StandaloneSubjectService standaloneSubjectService() {

		StandaloneSubjectService target = new StandaloneSubjectService();
		ProxyFactoryBean pfb = new ProxyFactoryBean();
		pfb.setTarget(target);
		pfb.setProxyTargetClass(true);
		pfb.addAdvice((MethodInterceptor) invocation -> {
			if (invocation.getMethod().getName().equals("findTopHiddenSubject")) {
				log.info("\n******************************************************");
				Object returobj = invocation.proceed();
				log.info("\n Top Hidden Subject via Aot proxy  : " + (Subject) returobj);
				log.info("\n######################################################");
				return returobj;
			} else {
				Object returobj = invocation.proceed();
				return returobj;
			}
		});

		Object proxy = pfb.getObject();
		return (StandaloneSubjectService) proxy;

	}

	// AOT Interface Proxy
	@Bean
	SubjectService subjectService() {

		SubjectService ss = ProxyFactory.getProxy(SubjectService.class, (MethodInterceptor) invocation -> {
			if (invocation.getMethod().getName().equals("findFirstHiddenSubject")) {
				return new Subject(420, "Hidden Subject - Dont Share", "Piuy Tcdq");
			}
			return invocation.proceed();// For others methods if any
		});

		return ss;

	}

	@Bean
	public ApplicationRunner resourceRunner(@Value("classpath:data/csvresource.csv") Resource csv) {
		return args -> {
			try {

				InputStreamReader isr = new InputStreamReader(csv.getInputStream());
				String csvString = FileCopyUtils.copyToString(isr);
				int linesinCSV = csvString.split("\r").length;
				log.info("\nLines in CSV : " + csv.getFilename() + " : " + linesinCSV);
			} catch (Exception e) {
				log.error("Error in resourceRunner  " + e);
			}

		};
	}

	@Bean
	public ApplicationRunner serializationRunner(@Value("file:///${user.home}/objectout") Resource objout) {

		return args -> {
			try {
				if (!objout.getFile().exists())
					objout.getFile().createNewFile();
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(objout.getFile()));
				Subject streamObj = new Subject(100, "ser", "sercity");
				oos.writeObject(streamObj);
				log.info("\nWrote Obj :: " + streamObj);

				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(objout.getFile()));
				streamObj = (Subject) ois.readObject();
				log.info("\nRead Obj :: " + streamObj);

			} catch (Exception e) {
				log.error("Error in serializationRunner  " + e);
			}

		};
	}

	@Bean
	public CommandLineRunner clr() {
		return args -> {
			repo.deleteAll()
					.thenMany(
							Flux.just("Peela", "Lal", "Hara")
									.map(n -> new Subject(null, n, n + "'s City"))
									.flatMap(repo::save))
					.thenMany(repo.findAll())
					.subscribe(System.out::println);
		};
	}

	@Bean
	public RouterFunction<ServerResponse> routerFunctions() {
		return RouterFunctions.route()
				.GET("/subjects", request -> ServerResponse.ok().body(repo.findAll(), Subject.class)).build();

	}

}

interface Subjectrepo extends ReactiveCrudRepository<Subject, Integer> {

}
