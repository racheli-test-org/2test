import sqlite3

def get_user_by_name(username):
    conn = sqlite3.connect('example.db')
    cursor = conn.cursor()

    # Vulnerable to SQL injection
    query = f"SELECT * FROM users WHERE username = '{username}'"
    cursor.execute(query)

    return cursor.fetchall()

# Example usage
user_input = "admin' OR '1'='1"
print(get_user_by_name(user_input))
