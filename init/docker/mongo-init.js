db = db.getSiblingDB('itsupport');

db.createUser({
  user: "",
  pwd: "", 
  roles: [
    { role: "readWrite", db: "itsupport" },
    { role: "dbAdmin", db: "itsupport" }
  ]
});
