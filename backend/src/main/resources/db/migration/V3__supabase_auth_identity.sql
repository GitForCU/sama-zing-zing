-- Explicit mapping to the identity provider; application roles remain in students/teachers.
ALTER TABLE app_users ADD COLUMN auth_user_id UUID UNIQUE;
