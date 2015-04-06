# CTE Bulk Id Strategy for Hibernate

Bulk Id Strategy implementation for Hibernate using CTE instead of Temporary Table, reducing concurrency on system tables every time Postgresql creates temp tables.

## Generated sql example

```
WITH ht_person ( doc_number ) as ( values (?), (?), (?), (?) ) UPDATE public.person SET name = 'Evandro', birth = '1987-11-26' WHERE ( doc_number ) IN ( SELECT doc_number FROM ht_person ); 
``` 

## How to use

It is very simple. 

Make the jar using

```
mvn package
```

Just add the JAR in your classpath and add the line below in your hibernate.cfg.xml

```
<property name="hibernate.hql.bulk_id_strategy">br.com.evandropires.hibernate.ctestrategy.CTEMultiTableBulkIdStrategy</property>
```
