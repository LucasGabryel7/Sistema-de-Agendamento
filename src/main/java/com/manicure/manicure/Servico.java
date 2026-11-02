package com.manicure.manicure;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 * Representa um serviço oferecido, como "Manicure" ou "Pedicure".
 * Ao estender PanacheEntity, a classe ganha o campo "id" e métodos
 * prontos como listAll(), persist() e findById().
 * Cada @Entity vira uma tabela no PostgreSQL.
 */
@Entity
public class Servico extends PanacheEntity {
    public String nome;
    public int duracaoMinutos;
    public double preco;
}