CREATE TABLE IF NOT EXISTS "members"
(
    "id"           uuid NOT NULL PRIMARY KEY,
    "organization" uuid NOT NULL,
    "name"         text
);

CREATE TABLE IF NOT EXISTS "password_credentials"
(
    "member_id" uuid NOT NULL,
    "user_name" text NOT NULL UNIQUE,
    "password" text NOT NULL,
    PRIMARY KEY ("user_name", "member_id")
);

CREATE TABLE IF NOT EXISTS "roles"
(
    "id"   uuid NOT NULL PRIMARY KEY,
    "name" text NOT null
);

CREATE TABLE IF NOT EXISTS "role_grants"
(
    "role_id"   uuid NOT NULL,
    "member_id" uuid NOT NULL,
    PRIMARY KEY ("role_id", "member_id")
);

CREATE TABLE IF NOT EXISTS "permission_grants" 
(
	"role_id" UUID NOT NULL,
	"permission_id" UUID NOT NULL,
	PRIMARY KEY("role_id", "permission_id")
);

CREATE TABLE IF NOT EXISTS "permissions" 
(
	"id" UUID NOT NULL PRIMARY KEY,
	"target" TEXT,
	"permission" INTEGER
);

