package org.mifos.mifos.util;

import java.util.List;

import org.hibernate.Session;
import org.mifos.accounts.acceptedpaymenttype.business.TransactionTypeEntity;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

public class ExtractMasterEntity {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classpath*:org/mifos/config/resources/persistenceContext.xml",
				"classpath:org/mifos/config/resources/dataSourceContext.xml");
		ctx.start();

		HibernateTransactionManager b = ctx.getBean(HibernateTransactionManager.class);

		Session session = b.getSessionFactory().openSession();

		session.beginTransaction();

		List<TransactionTypeEntity> el = session.createQuery("from TransactionTypeEntity").list();

		for(TransactionTypeEntity e : el) {
			System.out.println(e.getTransactionName());
		}



	}

}
