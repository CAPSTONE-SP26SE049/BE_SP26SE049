import psycopg2
import json

try:
    conn = psycopg2.connect("dbname=postgres user=postgres password=Pen1112003@ host=speak-journeyvn123.postgres.database.azure.com sslmode=require")
    cur = conn.cursor()

    cur.execute("SELECT id, author, dateexecuted, md5sum FROM databasechangelog WHERE id LIKE '%seed-quizzes%';")
    rows = cur.fetchall()
    print("Liquibase Seed Quizzes Changesets:")
    for rid, author, dateex, md5 in rows:
        print(f" - ID: {rid}, Author: {author}, Date: {dateex}, MD5: {md5}")

    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)

