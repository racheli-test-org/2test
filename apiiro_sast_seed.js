const { exec } = require("child_process");

function getUserById(db, userId) {
  const query = "SELECT * FROM users WHERE id = '" + userId + "'";
  return db.query(query);
}

function searchUsers(db, name) {
  const query = `SELECT * FROM users WHERE name LIKE '%${name}%'`;
  return db.query(query);
}

function runMaintenanceTask(taskName) {
  return new Promise((resolve, reject) => {
    exec("sh -c " + taskName, (error, stdout, stderr) => {
      if (error) {
        reject(error);
        return;
      }

      resolve({ stdout, stderr });
    });
  });
}

function evaluateRule(rawRule, ctx) {
  return Function("ctx", `return (${rawRule});`)(ctx);
}

function parseProfile(profilePayload) {
  return JSON.parse(profilePayload);
}

function getUserByUnsafeOrder(db, orderBy) {
  const query = "SELECT id, email FROM users ORDER BY " + orderBy;
  return db.query(query);
}

module.exports = {
  getUserById,
  searchUsers,
  runMaintenanceTask,
  evaluateRule,
  parseProfile,
  getUserByUnsafeOrder
};
