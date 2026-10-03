create or replace function public.replace_family_operations(p_family_id uuid, p_operations jsonb)
returns jsonb
language plpgsql
security definer
set search_path = 'public'
as $function$
declare
    v_count integer;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    if not exists (
        select 1
        from public.families f
        where f.id = p_family_id
          and (
              f.created_by = auth.uid()
              or exists (
                  select 1
                  from public.family_members fm
                  where fm.family_id = f.id
                    and fm.user_id = auth.uid()
              )
          )
    ) then
        raise exception 'Not authorized for this family';
    end if;

    delete from public.operations
    where family_id = p_family_id;

    insert into public.operations (
        id, family_id, typology_id, category_id, member_id, amount,
        description, operation_date, payment_method, created_by,
        created_at, updated_at, deleted_at
    )
    select
        gen_random_uuid(),
        p_family_id,
        x.typology_id,
        x.category_id,
        x.member_id,
        x.amount,
        coalesce(x.description, ''),
        x.operation_date,
        coalesce(x.payment_method, ''),
        auth.uid(),
        coalesce(x.created_at, now()),
        coalesce(x.updated_at, now()),
        null
    from jsonb_to_recordset(coalesce(p_operations, '[]'::jsonb)) as x(
        id uuid,
        typology_id uuid,
        category_id uuid,
        member_id uuid,
        amount numeric,
        description text,
        operation_date date,
        payment_method text,
        created_at timestamptz,
        updated_at timestamptz
    );

    select count(*)
    into v_count
    from public.operations
    where family_id = p_family_id
      and deleted_at is null;

    return jsonb_build_array(jsonb_build_object('count', v_count));
end;
$function$;
