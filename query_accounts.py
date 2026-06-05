import psycopg2

try:
    conn = psycopg2.connect("dbname=postgres user=postgres password=Pen1112003@ host=speak-journeyvn123.postgres.database.azure.com sslmode=require")
    cur = conn.cursor()

    cur.execute("SELECT id, email, role_code, is_active, last_activity_date, last_active_at, last_login_date, updated_at FROM account ORDER BY last_login_date DESC NULLS LAST, last_active_at DESC NULLS LAST LIMIT 10;")
    print("Latest active accounts:")
    for row in cur.fetchall():
        print(row)

    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
