package com.umc.pediupatrao.entity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditoria")
public class EventoAuditoria {
    @Id
    private String id;
    private String usuarioId;
    private String usuario;
    private String perfil;
    private Instant dataHora;
    private String tipoOperacao;
    private String entidade;
    private String entidadeId;
    private Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
    private Map<String, Object> valoresNovos = new LinkedHashMap<>();
    private String justificativa;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public Instant getDataHora() { return dataHora; }
    public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }
    public String getTipoOperacao() { return tipoOperacao; }
    public void setTipoOperacao(String tipoOperacao) { this.tipoOperacao = tipoOperacao; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }
    public Map<String, Object> getValoresAnteriores() { return valoresAnteriores; }
    public void setValoresAnteriores(Map<String, Object> valoresAnteriores) { this.valoresAnteriores = valoresAnteriores; }
    public Map<String, Object> getValoresNovos() { return valoresNovos; }
    public void setValoresNovos(Map<String, Object> valoresNovos) { this.valoresNovos = valoresNovos; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
}
