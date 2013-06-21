package com.iorga.iraj.util;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;


/**
 * QProfile is a Querydsl query type for Profile
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QProfile extends EntityPathBase<Profile> {

    private static final long serialVersionUID = -402159068;

    public static final QProfile profile = new QProfile("profile");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath label = createString("label");

    public final DateTimePath<java.util.Date> version = createDateTime("version", java.util.Date.class);

    public QProfile(final String variable) {
        super(Profile.class, forVariable(variable));
    }

    @SuppressWarnings("all")
    public QProfile(final Path<? extends Profile> path) {
        super((Class)path.getType(), path.getMetadata());
    }

    public QProfile(final PathMetadata<?> metadata) {
        super(Profile.class, metadata);
    }

}

