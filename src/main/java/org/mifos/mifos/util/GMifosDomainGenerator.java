package org.mifos.mifos.util;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.mifos.application.master.business.MasterDataEntity;
import org.mifos.framework.business.AbstractBusinessObject;
import org.mifos.framework.business.AbstractEntity;
import org.mifos.framework.util.helpers.Money;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

public class GMifosDomainGenerator {
	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/mifos/config/resources/persistenceContext.xml",
				"classpath:org/mifos/config/resources/dataSourceContext.xml");
		ctx.start();

		AnnotationSessionFactoryBean b = ctx.getBean(AnnotationSessionFactoryBean.class);
		Iterator<PersistentClass> i = b.getConfiguration().getClassMappings();

		List<GroovyDomainFile> domains = new ArrayList<GroovyDomainFile>();

		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		while (i.hasNext()) {
			PersistentClass p = i.next();

			GroovyDomainFile d = new GroovyDomainFile();

			Class<?> c = cl.loadClass(p.getClassName());
			d.className = c.getSimpleName();
			d.packageName = c.getPackage().getName();
			Class<?> s = c.getSuperclass();

			if(!isIgnored(s)) {
				d.imports.add(s.getName());
				d.superClass = s.getSimpleName();
			}

			Iterator<?> props = p.getPropertyIterator();

			while (props.hasNext()) {
				Property property = (Property) props.next();
				if (property.isBackRef()) {
					continue;
				}
				Class<?> fieldType = property.getType().getReturnedClass();
				fieldType = replaceIfMoney(fieldType);
				String fieldName = property.getName();
				d.fields.put(fieldName, fieldType.getSimpleName());
				if (!fieldType.isPrimitive() && !fieldType.isArray()) {
					d.imports.add(fieldType.getName());
				}
			}
			domains.add(d);
		}
		createDomainFiles(domains);
	}

	private static boolean isIgnored(Class<?> s) {
		List<Class<?>> ignored = new ArrayList<Class<?>>();
		ignored.add(Object.class);
		ignored.add(AbstractEntity.class);
		ignored.add(AbstractBusinessObject.class);
		ignored.add(MasterDataEntity.class);
		ignored.add(org.mifos.accounts.business.AccountActionDateEntity.class);
		ignored.add(org.mifos.accounts.productdefinition.business.AmountRange.class);
		ignored.add(org.mifos.accounts.productdefinition.business.InstallmentRange.class);
		ignored.add(org.mifos.accounts.productdefinition.business.LoanOfferingInstallmentRange.class);
		ignored.add(org.mifos.accounts.productdefinition.business.LoanAmountOption.class);
		ignored.add(org.mifos.accounts.business.AccountFeesActionDetailEntity.class);
		ignored.add(org.mifos.application.master.business.StateEntity.class);
		ignored.add(org.mifos.customers.business.CustomerPerformanceHistory.class);
		ignored.add(org.mifos.reports.cashconfirmationreport.BranchCashConfirmationInfoBO.class);
		ignored.add(org.mifos.reports.cashconfirmationreport.BranchCashConfirmationSubReport.class);
		return ignored.contains(s);
	}

	private static Class<?> replaceIfMoney(Class<?> fieldType) {
		if (fieldType.equals(Money.class)) {
			return BigDecimal.class;
		}
		return fieldType;
	}

	private static void createDomainFiles(List<GroovyDomainFile> domains)
			throws Exception {
		 String base = "/home/ugupta/Projects/Mifos/workspace/gmifos/grails-app/domain/";
		//String base = "/home/ugupta/Projects/Mifos/workspace/grmifos/src/";

		for (GroovyDomainFile gdf : domains) {
			gdf.className = removeEntityAndBO(gdf.className);
			gdf.packageName = removeBussinessPackage(gdf.packageName);
			File dir = new File(base + gdf.packageName.replace(".", "/") + "/");
			dir.mkdirs();
			File name = new File(base + gdf.packageName.replace(".", "/") + "/"
					+ gdf.className + ".groovy");
			name.createNewFile();
			FileWriter f = new FileWriter(name);
			StringBuffer p = new StringBuffer();

			p.append("package ").append(gdf.packageName).append("\n\n");
			for (String importName : gdf.imports) {
				if (!importName.startsWith("java.")) {
					importName = removeEntityAndBO(importName);
					importName = removeBussinessPackage(importName);
					p.append("import ").append(importName).append(";\n");
				}
			}

			p.append("\nclass ").append(gdf.className);
			if (gdf.superClass != null) {
				gdf.superClass = removeEntityAndBO(gdf.superClass);
				p.append(" extends ").append(gdf.superClass);
			}

			p.append(" {\n\n");

			for (Entry<String, String> field : gdf.fields.entrySet()) {
				String fieldValue = removeEntityAndBO(field.getValue());
				p.append("    ").append(fieldValue);
				p.append(" ").append(field.getKey()).append("\n\n");
			}

			p.append("}\n");

			String data = p.toString();

			System.out.print(data);

			f.write(data);
			f.flush();
			f.close();
		}
	}

	private static String removeBussinessPackage(String importName) {
		return importName.replace(".business", "");
	}

	private static String removeEntityAndBO(String className) {
		return className.replace("BO", "").replace("Entity", "");
	}
}



class GroovyDomainFile {
	String packageName;
	Set<String> imports = new HashSet<String>();
	String className;
	String superClass;
	Map<String, String> fields = new HashMap<String, String>();
}
