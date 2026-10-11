-- drop database if exists kinalllavesdb_in4cm;
create database if not exists kinalllavesdb_in4cm;
use kinalllavesdb_in4cm;

-- TABLAS DE SEGURIDAD --------------------------
create table if not exists usuarios(
	id_usuario int auto_increment primary key,
    nombre_usuario varchar(50) not null unique,
    nombre varchar(50) not null,
    apellido varchar(50) not null,
    correo varchar(100) not null unique,
    contrasena varchar(255) not null,
	estado boolean default true
);

create table if not exists roles(
	id_rol int auto_increment primary key,
	nombre varchar(50) not null unique,
	descripcion varchar(255)
);

create table permisos (
    id_permiso int auto_increment primary key,
    nombre_permiso varchar(50) not null unique,
    descripcion varchar(255)
);

-- TABLAS DE RELACIONES------------------------------
create table if not exists usuario_rol(
	id_usuario int,
    id_rol int,
    primary key(id_usuario, id_rol),
    constraint fk_usuario_rol foreign key (id_usuario) references usuarios(id_usuario) on delete cascade,
    constraint fk_id_rol foreign key (id_rol) references roles(id_rol) on delete cascade
);

create table rol_permiso (
    id_rol int,
    id_permiso int,
    primary key (id_rol, id_permiso),
    constraint fk_id_rol_permiso foreign key (id_rol) references roles(id_rol) on delete cascade,
    constraint fk_id_permiso foreign key (id_permiso) references permisos(id_permiso) on delete cascade
);

-- TABLAS MAESTRAS---------------------------
create table empleados (
    id_empleado int auto_increment primary key,
    id_usuario int unique,
    nombre varchar(100) not null,
    apellido varchar(100) not null,
    cui varchar(20) not null unique,
    rfid varchar(50) unique null, 
    constraint fk_id_usuario foreign key (id_usuario) references usuarios(id_usuario) on delete set null
);
 
create table salones (
    id_salon int auto_increment primary key,
    codigo_salon varchar(20) not null unique,
    nombre varchar(50) not null,
    ubicacion varchar(100)
);
 
create table llaves (
    id_llave int auto_increment primary key,
    codigo_llave varchar(20) not null unique,
    id_salon int not null,
    estado varchar(30) default 'Disponible',
    constraint fk_id_salon foreign key (id_salon) references salones(id_salon) on delete cascade
);