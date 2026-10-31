-- Serviços de exemplo, inseridos ao iniciar a aplicação
INSERT INTO Servico(id, nome, duracaoMinutos, preco) VALUES (nextval('Servico_SEQ'), 'Manicure', 45, 35.00);
INSERT INTO Servico(id, nome, duracaoMinutos, preco) VALUES (nextval('Servico_SEQ'), 'Pedicure', 60, 45.00);
INSERT INTO Servico(id, nome, duracaoMinutos, preco) VALUES (nextval('Servico_SEQ'), 'Manicure + Pedicure', 100, 70.00);