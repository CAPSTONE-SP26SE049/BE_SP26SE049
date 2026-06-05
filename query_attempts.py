import psycopg2

try:
    conn = psycopg2.connect("dbname=postgres user=postgres password=Pen1112003@ host=speak-journeyvn123.postgres.database.azure.com sslmode=require")
    cur = conn.cursor()

    cur.execute("SELECT id, created_at, consent_given, dialect, is_correct, groq_score, asr_score FROM speaking_attempt WHERE account_id = '860310be-937b-4b49-8686-5f7c412faacc' ORDER BY created_at DESC;")
    print("Attempts for lequocbaotxdh@gmail.com:")
    for row in cur.fetchall():
        print(row)

    cur.close()
    conn.close()
except Exception as e:
    print("Error:", e)
